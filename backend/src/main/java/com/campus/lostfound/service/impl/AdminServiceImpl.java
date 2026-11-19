package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.AdminUserQuery;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.AuditRow;
import com.campus.lostfound.dto.CategoryDTO;
import com.campus.lostfound.dto.CategoryStatRow;
import com.campus.lostfound.dto.DailyCountRow;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.AuditStatusEnum;
import com.campus.lostfound.enums.ClaimStatusEnum;
import com.campus.lostfound.enums.ItemStatusEnum;
import com.campus.lostfound.enums.ItemTypeEnum;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.enums.RoleEnum;
import com.campus.lostfound.enums.UserStatusEnum;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.AdminService;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.AdminUserVO;
import com.campus.lostfound.vo.AuditItemVO;
import com.campus.lostfound.vo.AuditResultVO;
import com.campus.lostfound.vo.CategoryAdminVO;
import com.campus.lostfound.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台实现（详细设计 4.6）。
 * <p>
 * 审核用条件更新防重复；分类删除前校验引用；禁用账号即时生效由拦截器保证；
 * 统计一次聚合 + 应用层补零，SQL 数量固定。
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final LostItemMapper itemMapper;
    private final ItemImageMapper imageMapper;
    private final ClaimMapper claimMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    // ---------------- U6.1 信息审核 ----------------

    @Override
    public PageResult<AuditItemVO> auditPage(int page, int size, Integer auditStatus, String keyword) {
        SecurityUtil.requireAdmin();
        int status = auditStatus == null ? AuditStatusEnum.PENDING.getCode() : auditStatus;
        String kw = keyword == null ? "" : keyword.trim();
        String like = kw.isEmpty() ? null : "%" + escapeLike(kw) + "%";
        IPage<AuditRow> result = itemMapper.pageForAudit(new Page<>(page, size), status, like);
        List<AuditRow> rows = result.getRecords();
        if (rows.isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<Long, List<String>> images = imagesOf(rows.stream().map(AuditRow::getId).collect(Collectors.toSet()));
        List<AuditItemVO> vos = rows.stream()
                .map(r -> new AuditItemVO(r.getId(), r.getTitle(), r.getType(), ItemTypeEnum.textOf(r.getType()),
                        r.getCategoryId(), r.getCategoryName(), r.getPublisherNickname(), r.getDescription(),
                        images.getOrDefault(r.getId(), List.of()), r.getAuditStatus(),
                        AuditStatusEnum.textOf(r.getAuditStatus()), r.getCreatedAt()))
                .toList();
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(),
                (int) result.getPages(), vos);
    }

    @Override
    @Transactional
    public AuditResultVO audit(Long id, AuditDTO dto) {
        SecurityUtil.requireAdmin();
        LostItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);                    // 2004
        }
        if (!Integer.valueOf(AuditStatusEnum.PENDING.getCode()).equals(item.getAuditStatus())
                || Integer.valueOf(ItemStatusEnum.CLOSED.getCode()).equals(item.getItemStatus())) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);               // 2003
        }
        Integer target = dto.getAuditStatus();
        String reason = dto.getRejectReason();
        if (Integer.valueOf(AuditStatusEnum.REJECTED.getCode()).equals(target)
                && (!StringUtils.hasText(reason) || reason.length() > 100)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回必须填写 1~100 字原因");
        }

        int rows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                .eq(LostItem::getId, id)
                .eq(LostItem::getAuditStatus, AuditStatusEnum.PENDING.getCode())
                .set(LostItem::getAuditStatus, target)
                .set(LostItem::getRejectReason,
                        Integer.valueOf(AuditStatusEnum.REJECTED.getCode()).equals(target) ? reason : null));
        if (rows != 1) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);               // 重复审核
        }
        String text = Integer.valueOf(AuditStatusEnum.APPROVED.getCode()).equals(target)
                ? "你发布的「" + item.getTitle() + "」已通过审核"
                : "你发布的「" + item.getTitle() + "」未通过审核：" + reason;
        messageService.push(item.getUserId(), MessageTypeEnum.AUDIT, text, id);

        return new AuditResultVO(id, target, item.getItemStatus(), LocalDateTime.now());
    }

    // ---------------- U6.2 分类管理 ----------------

    @Override
    public List<CategoryAdminVO> categories() {
        SecurityUtil.requireAdmin();
        Map<Long, Long> counts = itemCounts();
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort))
                .stream()
                .map(c -> new CategoryAdminVO(c.getId(), c.getName(), c.getSort(),
                        counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public CategoryAdminVO createCategory(CategoryDTO dto) {
        SecurityUtil.requireAdmin();
        String name = dto.getName().trim();
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getName, name)) > 0) {
            throw new BusinessException(ResultCode.CATEGORY_EXISTS);                   // 5001
        }
        Category category = new Category();
        category.setName(name);
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return new CategoryAdminVO(category.getId(), category.getName(), category.getSort(), 0L);
    }

    @Override
    @Transactional
    public CategoryAdminVO updateCategory(Long id, CategoryDTO dto) {
        SecurityUtil.requireAdmin();
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);                // 5003
        }
        String name = dto.getName().trim();
        long duplicated = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)
                .ne(Category::getId, id));
        if (duplicated > 0) {
            throw new BusinessException(ResultCode.CATEGORY_EXISTS);                   // 5001
        }
        category.setName(name);
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        categoryMapper.updateById(category);
        return new CategoryAdminVO(category.getId(), category.getName(), category.getSort(),
                itemCounts().getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        SecurityUtil.requireAdmin();
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);                // 5003
        }
        Long used = itemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BusinessException(ResultCode.CATEGORY_IN_USE,
                    "该分类下仍有 " + used + " 条失物信息，无法删除");                    // 5002
        }
        categoryMapper.deleteById(id);
    }

    // ---------------- U6.3 用户管理 ----------------

    @Override
    public PageResult<AdminUserVO> users(AdminUserQuery query) {
        SecurityUtil.requireAdmin();
        String kw = query.trimmedKeyword();
        IPage<User> page = userMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                new LambdaQueryWrapper<User>()
                        .eq(query.getRole() != null, User::getRole, query.getRole())
                        .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                        .and(!kw.isEmpty(), w -> w.like(User::getStudentNo, kw).or().like(User::getNickname, kw))
                        .orderByDesc(User::getCreatedAt));
        List<User> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }
        Set<Long> ids = records.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Long> itemCounts = groupCounts(itemMapper.selectMaps(new QueryWrapper<LostItem>()
                .select("user_id", "COUNT(*) AS cnt").in("user_id", ids).groupBy("user_id")));
        Map<Long, Long> claimCounts = groupCounts(claimMapper.selectMaps(new QueryWrapper<Claim>()
                .select("applicant_id", "COUNT(*) AS cnt").in("applicant_id", ids).groupBy("applicant_id")));

        List<AdminUserVO> vos = records.stream()
                .map(u -> new AdminUserVO(u.getId(), u.getStudentNo(), u.getNickname(), u.getPhone(),
                        u.getRole(),
                        Integer.valueOf(RoleEnum.ADMIN.getCode()).equals(u.getRole())
                                ? RoleEnum.ADMIN.getDesc() : RoleEnum.STUDENT.getDesc(),
                        u.getStatus(),
                        Integer.valueOf(UserStatusEnum.DISABLED.getCode()).equals(u.getStatus())
                                ? UserStatusEnum.DISABLED.getDesc() : UserStatusEnum.NORMAL.getDesc(),
                        itemCounts.getOrDefault(u.getId(), 0L),
                        claimCounts.getOrDefault(u.getId(), 0L),
                        u.getCreatedAt()))
                .toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        SecurityUtil.requireAdmin();
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);                    // 2004
        }
        if (target.getId().equals(SecurityUtil.getUserId())
                || Integer.valueOf(RoleEnum.ADMIN.getCode()).equals(target.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);                     // 2002
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getStatus, status));
    }

    // ---------------- U6.4 数据统计 ----------------

    @Override
    public StatsVO stats(Integer days) {
        SecurityUtil.requireAdmin();
        int range = days == null ? 30 : Math.max(7, Math.min(30, days));

        long total = itemMapper.selectCount(null);
        long pending = itemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getAuditStatus, AuditStatusEnum.PENDING.getCode())
                .ne(LostItem::getItemStatus, ItemStatusEnum.CLOSED.getCode()));
        long completedClaims = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getStatus, ClaimStatusEnum.APPROVED.getCode()));

        long foundTotal = itemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getType, ItemTypeEnum.FOUND.getCode())
                .eq(LostItem::getAuditStatus, AuditStatusEnum.APPROVED.getCode()));
        long foundDone = itemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getType, ItemTypeEnum.FOUND.getCode())
                .eq(LostItem::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                .eq(LostItem::getItemStatus, ItemStatusEnum.COMPLETED.getCode()));
        double returnRate = rate(foundDone, foundTotal);

        List<StatsVO.CategoryStat> categoryStats = new ArrayList<>();
        for (CategoryStatRow row : itemMapper.categoryStats()) {
            long itemCount = row.getItemCount() == null ? 0 : row.getItemCount();
            long done = row.getCompletedCount() == null ? 0 : row.getCompletedCount();
            categoryStats.add(new StatsVO.CategoryStat(row.getCategoryId(), row.getCategoryName(),
                    itemCount, done, rate(done, itemCount)));
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(range - 1L);
        Map<LocalDate, Long> daily = new HashMap<>();
        for (DailyCountRow row : itemMapper.dailyCounts(start.atStartOfDay())) {
            if (row.getDay() != null) {
                daily.put(row.getDay(), row.getTotal() == null ? 0 : row.getTotal());
            }
        }
        List<StatsVO.TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            LocalDate date = start.plusDays(i);
            trend.add(new StatsVO.TrendPoint(date.toString(), daily.getOrDefault(date, 0L)));
        }

        return new StatsVO(total, pending, completedClaims, returnRate, categoryStats, trend);
    }

    // ---------------- 内部工具 ----------------

    private Map<Long, Long> itemCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (CategoryStatRow row : itemMapper.categoryStats()) {
            counts.put(row.getCategoryId(), row.getItemCount() == null ? 0 : row.getItemCount());
        }
        return counts;
    }

    private Map<Long, List<String>> imagesOf(Collection<Long> itemIds) {
        return imageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .orderByAsc(ItemImage::getSort))
                .stream()
                .collect(Collectors.groupingBy(ItemImage::getItemId,
                        Collectors.mapping(ItemImage::getUrl, Collectors.toList())));
    }

    private Map<Long, Long> groupCounts(List<Map<String, Object>> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object key = row.values().stream().filter(v -> v instanceof Number).findFirst().orElse(null);
            Object cnt = row.get("cnt");
            if (key instanceof Number k) {
                result.put(k.longValue(), cnt instanceof Number c ? c.longValue() : 0L);
            }
        }
        return result;
    }

    private double rate(long done, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(done * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
