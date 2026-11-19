package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.dto.ItemSaveDTO;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.AuditStatusEnum;
import com.campus.lostfound.enums.ClaimStatusEnum;
import com.campus.lostfound.enums.ItemStatusEnum;
import com.campus.lostfound.enums.ItemTypeEnum;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.util.ItemStatusMachine;
import com.campus.lostfound.util.MaskUtil;
import com.campus.lostfound.vo.CategoryVO;
import com.campus.lostfound.vo.CommentBriefVO;
import com.campus.lostfound.vo.ItemCardVO;
import com.campus.lostfound.vo.ItemDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 失物信息与浏览检索实现（详细设计 4.2、4.3）。
 * <p>
 * 约定：状态变更一律先过 {@link ItemStatusMachine}；更新用条件更新防并发；列表批量补齐关联信息避免 N+1。
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    /** 图片地址白名单：/uploads/yyyyMM/32 位十六进制.jpg|jpeg|png（挡外链与路径穿越）。 */
    private static final Pattern IMAGE_URL =
            Pattern.compile("^/uploads/\\d{6}/[0-9a-f]{32}\\.(jpg|jpeg|png)$");

    private final LostItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final ClaimMapper claimMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    // ---------------- U2.1 发布 ----------------

    @Override
    @Transactional
    public Long create(ItemSaveDTO dto) {
        validateType(dto.getType());
        requireCategory(dto.getCategoryId());
        validateHappenTime(dto.getHappenTime());
        validateImages(dto.getImageUrls(), null);

        LostItem item = new LostItem();
        item.setUserId(SecurityUtil.getUserId());
        applyFields(item, dto);
        item.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        item.setItemStatus(ItemStatusEnum.ONGOING.getCode());
        item.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        saveImages(item.getId(), dto.getImageUrls());
        return item.getId();
    }

    // ---------------- U2.3 编辑 ----------------

    @Override
    @Transactional
    public void update(Long id, ItemSaveDTO dto) {
        LostItem item = requireItem(id);
        requireOwnerOrAdmin(item);
        if (!Integer.valueOf(ItemStatusEnum.ONGOING.getCode()).equals(item.getItemStatus())) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);       // 处理中/已完成/已关闭不可编辑
        }
        validateType(dto.getType());
        requireCategory(dto.getCategoryId());
        validateHappenTime(dto.getHappenTime());
        validateImages(dto.getImageUrls(), id);

        applyFields(item, dto);
        item.setAuditStatus(AuditStatusEnum.PENDING.getCode());                // 修改后重新审核
        item.setRejectReason(null);
        itemMapper.updateById(item);

        if (dto.getImageUrls() != null) {
            imageMapper.delete(new LambdaQueryWrapper<ItemImage>().eq(ItemImage::getItemId, id));
            saveImages(id, dto.getImageUrls());
        }
    }

    // ---------------- U2.4 关闭 ----------------

    @Override
    @Transactional
    public void close(Long id, String reason) {
        LostItem item = requireItem(id);
        boolean admin = SecurityUtil.isAdmin();
        if (!item.getUserId().equals(SecurityUtil.getUserId()) && !admin) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (admin && (!StringUtils.hasText(reason) || reason.length() > 100)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "管理员下架必须填写 1~100 字理由");
        }
        ItemStatusMachine.checkClose(item.getItemStatus());
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                .eq(LostItem::getId, id)
                .eq(LostItem::getItemStatus, item.getItemStatus())
                .set(LostItem::getItemStatus, ItemStatusEnum.CLOSED.getCode())
                .set(LostItem::getCloseReason, reason));
        if (rows != 1) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);       // 并发关闭
        }

        List<Claim> pending = claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, id)
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode()));
        for (Claim c : pending) {
            claimMapper.update(null, new LambdaUpdateWrapper<Claim>()
                    .eq(Claim::getId, c.getId())
                    .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode())
                    .set(Claim::getStatus, ClaimStatusEnum.REJECTED.getCode())
                    .set(Claim::getReply, "信息已关闭，申请自动取消"));
            messageService.push(c.getApplicantId(), MessageTypeEnum.CLAIM_RESULT,
                    "该信息已关闭，你的认领申请自动取消", id);
        }
        messageService.push(item.getUserId(), MessageTypeEnum.SYSTEM, "你发布的信息已关闭", id);
    }

    // ---------------- U2.5 寻物状态推进 ----------------

    @Override
    @Transactional
    public void advance(Long id, Integer targetStatus) {
        LostItem item = requireItem(id);
        requireOwnerOrAdmin(item);
        if (!Integer.valueOf(ItemTypeEnum.LOST.getCode()).equals(item.getType())
                || !Integer.valueOf(AuditStatusEnum.APPROVED.getCode()).equals(item.getAuditStatus())) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);       // 仅已过审的寻物
        }
        ItemStatusMachine.checkItem(item.getItemStatus(), targetStatus);
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                .eq(LostItem::getId, id)
                .eq(LostItem::getItemStatus, item.getItemStatus())
                .set(LostItem::getItemStatus, targetStatus));
        if (rows != 1) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);
        }
        messageService.push(item.getUserId(), MessageTypeEnum.SYSTEM,
                Integer.valueOf(ItemStatusEnum.PROCESSING.getCode()).equals(targetStatus)
                        ? "已标记为处理中（已联系到拾到者）" : "已标记为已完成（物品已找回）", id);
    }

    // ---------------- U3.1 / U3.2 列表与检索 ----------------

    @Override
    public PageResult<ItemCardVO> page(ItemQuery query) {
        validateType(query.getType());
        String keyword = query.trimmedKeyword();
        IPage<LostItem> page;
        if (!keyword.isEmpty()) {
            page = itemMapper.search(new Page<>(query.safePage(), query.safeSize()),
                    "%" + escapeLike(keyword) + "%", query.getType(), query.getCategoryId());
        } else {
            page = itemMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                    new LambdaQueryWrapper<LostItem>()
                            .eq(LostItem::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                            .ne(LostItem::getItemStatus, ItemStatusEnum.CLOSED.getCode())
                            .eq(query.getType() != null, LostItem::getType, query.getType())
                            .eq(query.getCategoryId() != null, LostItem::getCategoryId, query.getCategoryId())
                            .eq(query.getItemStatus() != null, LostItem::getItemStatus, query.getItemStatus())
                            .orderByDesc(LostItem::getCreatedAt));
        }
        List<LostItem> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }

        Map<Long, String> categories = categoryNames(records.stream()
                .map(LostItem::getCategoryId).collect(Collectors.toSet()));
        Map<Long, String> covers = covers(records.stream().map(LostItem::getId).collect(Collectors.toSet()));
        List<ItemCardVO> vos = records.stream()
                .map(i -> new ItemCardVO(i.getId(), i.getType(), ItemTypeEnum.textOf(i.getType()),
                        i.getTitle(), covers.get(i.getId()), categories.get(i.getCategoryId()),
                        i.getHappenPlace(), i.getHappenTime(), i.getItemStatus(),
                        ItemStatusEnum.textOf(i.getItemStatus()), i.getCreatedAt()))
                .toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    // ---------------- U3.3 详情 ----------------

    @Override
    public ItemDetailVO detail(Long id) {
        LostItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        LoginUser viewer = SecurityUtil.get();
        boolean owner = viewer != null && item.getUserId().equals(viewer.id());
        boolean admin = SecurityUtil.isAdmin();
        if (!Integer.valueOf(AuditStatusEnum.APPROVED.getCode()).equals(item.getAuditStatus())
                && !owner && !admin) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);            // 未过审对他人不可见
        }

        Category category = categoryMapper.selectById(item.getCategoryId());
        User publisher = userMapper.selectById(item.getUserId());
        List<String> images = imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, id)
                        .orderByAsc(ItemImage::getSort))
                .stream().map(ItemImage::getUrl).toList();
        List<CommentBriefVO> comments = comments(id);
        Integer myClaimStatus = myClaimStatus(id, viewer);

        String contact = owner || admin ? item.getContact() : MaskUtil.maskPhone(item.getContact());
        ItemDetailVO.Publisher publisherVO = publisher == null ? null
                : new ItemDetailVO.Publisher(publisher.getId(), publisher.getNickname(),
                MaskUtil.maskStudentNo(publisher.getStudentNo()));

        return new ItemDetailVO(item.getId(), item.getType(), ItemTypeEnum.textOf(item.getType()),
                item.getTitle(), item.getDescription(), item.getCategoryId(),
                category == null ? null : category.getName(), contact,
                item.getHappenTime(), item.getHappenPlace(),
                item.getAuditStatus(), AuditStatusEnum.textOf(item.getAuditStatus()),
                item.getItemStatus(), ItemStatusEnum.textOf(item.getItemStatus()),
                item.getCloseReason(), publisherVO, images, comments, myClaimStatus,
                item.getCreatedAt());
    }

    @Override
    public List<CategoryVO> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort))
                .stream().map(c -> new CategoryVO(c.getId(), c.getName(), c.getSort())).toList();
    }

    // ---------------- 内部工具 ----------------

    private LostItem requireItem(Long id) {
        LostItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        return item;
    }

    private void requireOwnerOrAdmin(LostItem item) {
        boolean owner = item.getUserId().equals(SecurityUtil.getUserId());
        if (!owner && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);        // 5003
        }
    }

    private void validateType(Integer type) {
        if (type != null && type != ItemTypeEnum.LOST.getCode() && type != ItemTypeEnum.FOUND.getCode()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "类型只能是 0 寻物或 1 拾物");
        }
    }

    private void validateHappenTime(LocalDateTime happenTime) {
        if (happenTime != null && happenTime.isAfter(LocalDateTime.now().plusDays(1))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发生时间不能晚于当前时间");
        }
    }

    /** 图片校验（评审 P0-5）：数量、白名单正则、文件存在、未被其他信息引用。 */
    private void validateImages(List<String> urls, Long currentItemId) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        if (urls.size() > 3) {
            throw new BusinessException(ResultCode.IMAGE_TOO_MANY);
        }
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads");
        for (String url : urls) {
            if (url == null || !IMAGE_URL.matcher(url).matches()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片地址非法，请重新上传");
            }
            if (!Files.exists(uploadRoot.resolve(url.substring("/uploads/".length())))) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片不存在，请重新上传");
            }
            LambdaQueryWrapper<ItemImage> wrapper = new LambdaQueryWrapper<ItemImage>()
                    .eq(ItemImage::getUrl, url);
            if (currentItemId != null) {
                wrapper.ne(ItemImage::getItemId, currentItemId);
            }
            if (imageMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片已被其他信息使用");
            }
        }
    }

    private void applyFields(LostItem item, ItemSaveDTO dto) {
        item.setType(dto.getType());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setHappenTime(dto.getHappenTime());
        item.setHappenPlace(dto.getHappenPlace());
        item.setContact(dto.getContact());
    }

    private void saveImages(Long itemId, List<String> urls) {
        if (urls == null) {
            return;
        }
        int sort = 0;
        for (String url : urls) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(url);
            image.setSort(sort++);
            imageMapper.insert(image);
        }
    }

    private Map<Long, String> categoryNames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, String> covers(Collection<Long> itemIds) {
        return imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .eq(ItemImage::getSort, 0))
                .stream()
                .collect(Collectors.toMap(ItemImage::getItemId, ItemImage::getUrl, (a, b) -> a));
    }

    private List<CommentBriefVO> comments(Long itemId) {
        List<Comment> list = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getItemId, itemId)
                .orderByAsc(Comment::getCreatedAt)
                .last("LIMIT 10"));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(list.stream()
                        .map(Comment::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        return list.stream().map(c -> new CommentBriefVO(c.getId(), c.getUserId(),
                users.containsKey(c.getUserId()) ? users.get(c.getUserId()).getNickname() : null,
                c.getContent(), c.getCreatedAt())).toList();
    }

    private Integer myClaimStatus(Long itemId, LoginUser viewer) {
        if (viewer == null) {
            return null;
        }
        Claim claim = claimMapper.selectOne(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, itemId)
                .eq(Claim::getApplicantId, viewer.id())
                .last("LIMIT 1"));
        return claim == null ? null : claim.getStatus();
    }

    /** LIKE 通配符转义，避免用户输入 % 导致全表扫描。 */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
