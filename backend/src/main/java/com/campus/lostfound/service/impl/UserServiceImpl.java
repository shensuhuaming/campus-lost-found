package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageQuery;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.PasswordDTO;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.AuditStatusEnum;
import com.campus.lostfound.enums.ClaimStatusEnum;
import com.campus.lostfound.enums.ItemStatusEnum;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.UserService;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.CommentVO;
import com.campus.lostfound.vo.ItemListVO;
import com.campus.lostfound.vo.ProfileVO;
import com.campus.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人中心实现（详细设计 4.1.4 ~ 4.1.6）。
 * <p>
 * 列表采用"先分页查主表、再批量补齐关联信息"的方式，避免 N+1 查询。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final LostItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final ClaimMapper claimMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileVO getProfile() {
        Long uid = SecurityUtil.getUserId();
        User user = requireUser(uid);
        long published = itemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getUserId, uid));
        long claiming = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getApplicantId, uid));
        long claimingPending = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getApplicantId, uid)
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode()));
        long comments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, uid));
        return ProfileVO.of(user, new ProfileVO.Stats(published, claiming, claimingPending, comments));
    }

    @Override
    @Transactional
    public UserVO updateProfile(ProfileDTO dto) {
        Long uid = SecurityUtil.getUserId();
        User user = requireUser(uid);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        userMapper.updateById(user);
        return UserVO.of(user);
    }

    @Override
    @Transactional
    public void changePassword(PasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
        }
        User user = requireUser(SecurityUtil.getUserId());
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);          // 1005
        }
        int rows = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .eq(User::getPassword, user.getPassword())                       // 以旧摘要为乐观条件
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword())));
        if (rows != 1) {                                                          // 并发改密
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
    }

    @Override
    public PageResult<ItemListVO> myItems(PageQuery query, Integer auditStatus, Integer itemStatus) {
        Long uid = SecurityUtil.getUserId();
        IPage<LostItem> page = itemMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                new LambdaQueryWrapper<LostItem>()
                        .eq(LostItem::getUserId, uid)
                        .eq(auditStatus != null, LostItem::getAuditStatus, auditStatus)
                        .eq(itemStatus != null, LostItem::getItemStatus, itemStatus)
                        .orderByDesc(LostItem::getCreatedAt));
        List<LostItem> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }

        Set<Long> itemIds = records.stream().map(LostItem::getId).collect(Collectors.toSet());
        Map<Long, String> categoryNames = categoryNames(records.stream()
                .map(LostItem::getCategoryId).collect(Collectors.toSet()));
        Map<Long, String> covers = coverImages(itemIds);
        Map<Long, Long> claimCounts = claimCounts(itemIds);

        List<ItemListVO> vos = records.stream()
                .map(i -> new ItemListVO(
                        i.getId(), i.getType(), i.getTitle(),
                        categoryNames.get(i.getCategoryId()),
                        covers.get(i.getId()),
                        i.getAuditStatus(), AuditStatusEnum.textOf(i.getAuditStatus()),
                        i.getRejectReason(),
                        i.getItemStatus(), ItemStatusEnum.textOf(i.getItemStatus()),
                        claimCounts.getOrDefault(i.getId(), 0L),
                        i.getCreatedAt()))
                .toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    @Override
    public PageResult<ClaimVO> myClaims(PageQuery query, Integer status) {
        Long uid = SecurityUtil.getUserId();
        IPage<Claim> page = claimMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                new LambdaQueryWrapper<Claim>()
                        .eq(Claim::getApplicantId, uid)
                        .eq(status != null, Claim::getStatus, status)
                        .orderByDesc(Claim::getCreatedAt));
        List<Claim> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }
        Map<Long, LostItem> items = itemsById(records.stream()
                .map(Claim::getItemId).collect(Collectors.toSet()));

        List<ClaimVO> vos = records.stream().map(c -> {
            LostItem item = items.get(c.getItemId());
            return new ClaimVO(c.getId(), c.getItemId(),
                    item == null ? null : item.getTitle(),
                    item == null ? null : item.getItemStatus(),
                    c.getDetailDesc(), c.getStatus(), ClaimStatusEnum.textOf(c.getStatus()),
                    c.getReply(), c.getCreatedAt());
        }).toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    @Override
    public PageResult<CommentVO> myComments(PageQuery query) {
        Long uid = SecurityUtil.getUserId();
        IPage<Comment> page = commentMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, uid)
                        .orderByDesc(Comment::getCreatedAt));
        List<Comment> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }
        Map<Long, LostItem> items = itemsById(records.stream()
                .map(Comment::getItemId).collect(Collectors.toSet()));

        List<CommentVO> vos = records.stream().map(c -> {
            LostItem item = items.get(c.getItemId());
            return new CommentVO(c.getId(), c.getItemId(),
                    item == null ? null : item.getTitle(),
                    c.getContent(), c.getCreatedAt());
        }).toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    // ---------------- 内部工具 ----------------

    private User requireUser(Long uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);              // 2004
        }
        return user;
    }

    private Map<Long, String> categoryNames(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    /** 封面图取 sort=0 的那张。 */
    private Map<Long, String> coverImages(Collection<Long> itemIds) {
        return imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .eq(ItemImage::getSort, 0))
                .stream()
                .collect(Collectors.toMap(ItemImage::getItemId, ItemImage::getUrl, (a, b) -> a));
    }

    /** 一次分组查询统计每条信息的认领数，避免 N+1。 */
    private Map<Long, Long> claimCounts(Collection<Long> itemIds) {
        List<Map<String, Object>> rows = claimMapper.selectMaps(new QueryWrapper<Claim>()
                .select("item_id", "COUNT(*) AS cnt")
                .in("item_id", itemIds)
                .groupBy("item_id"));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("item_id");
            Object cnt = row.get("cnt");
            if (id instanceof Number n) {
                result.put(n.longValue(), cnt instanceof Number c ? c.longValue() : 0L);
            }
        }
        return result;
    }

    private Map<Long, LostItem> itemsById(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(LostItem::getId, Function.identity(), (a, b) -> a));
    }
}
