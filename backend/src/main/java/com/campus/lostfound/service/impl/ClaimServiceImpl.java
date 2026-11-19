package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.ClaimAdminRow;
import com.campus.lostfound.dto.ClaimCreateDTO;
import com.campus.lostfound.dto.ClaimQuery;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.AuditStatusEnum;
import com.campus.lostfound.enums.ClaimStatusEnum;
import com.campus.lostfound.enums.ItemStatusEnum;
import com.campus.lostfound.enums.ItemTypeEnum;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.util.ItemStatusMachine;
import com.campus.lostfound.vo.ClaimAdminVO;
import com.campus.lostfound.vo.ClaimResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认领服务实现（详细设计 4.4）。
 * <p>
 * 并发正确性靠三层：状态机校验、条件更新（影响行数判断）、唯一索引兜底；
 * 核验通过时"物品置已完成 + 本申请通过 + 其余拒绝"在同一事务内完成。
 */
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimMapper claimMapper;
    private final LostItemMapper itemMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    // ---------------- U4.2 发起认领 ----------------

    @Override
    @Transactional
    public ClaimResultVO create(ClaimCreateDTO dto) {
        LostItem item = itemMapper.selectById(dto.getItemId());
        if (item == null || !Integer.valueOf(AuditStatusEnum.APPROVED.getCode()).equals(item.getAuditStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);                       // 2004
        }
        Integer status = item.getItemStatus();
        if (Integer.valueOf(ItemStatusEnum.COMPLETED.getCode()).equals(status)
                || Integer.valueOf(ItemStatusEnum.CLOSED.getCode()).equals(status)) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);                  // 2003
        }
        Long uid = SecurityUtil.getUserId();
        if (item.getUserId().equals(uid)) {
            throw new BusinessException(ResultCode.CLAIM_SELF_NOT_ALLOWED);               // 3001
        }
        if (!Integer.valueOf(ItemTypeEnum.FOUND.getCode()).equals(item.getType())) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL, "只能认领拾物信息");
        }
        if (claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, dto.getItemId())
                .eq(Claim::getApplicantId, uid)) > 0) {
            throw new BusinessException(ResultCode.CLAIM_EXISTS);                         // 3003
        }

        Claim claim = new Claim();
        claim.setItemId(dto.getItemId());
        claim.setApplicantId(uid);
        claim.setDetailDesc(dto.getDetailDesc());
        claim.setStatus(ClaimStatusEnum.PENDING.getCode());
        claim.setCreatedAt(LocalDateTime.now());
        try {
            claimMapper.insert(claim);
        } catch (DuplicateKeyException e) {                                                // 唯一索引兜底
            throw new BusinessException(ResultCode.CLAIM_EXISTS);
        }

        // 进行中 → 处理中（已处理中则跳过，不影响提交）
        int newStatus = item.getItemStatus();
        if (Integer.valueOf(ItemStatusEnum.ONGOING.getCode()).equals(status)) {
            int rows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                    .eq(LostItem::getId, item.getId())
                    .eq(LostItem::getItemStatus, ItemStatusEnum.ONGOING.getCode())
                    .set(LostItem::getItemStatus, ItemStatusEnum.PROCESSING.getCode()));
            if (rows == 1) {
                newStatus = ItemStatusEnum.PROCESSING.getCode();
            }
        }

        String nickname = nicknameOf(uid);
        messageService.push(item.getUserId(), MessageTypeEnum.CLAIM_APPLY,
                nickname + " 申请认领你发布的「" + item.getTitle() + "」", item.getId());
        return new ClaimResultVO(claim.getId(), newStatus);
    }

    // ---------------- U4.3 核验通过 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClaimResultVO approve(Long claimId, String reply) {
        SecurityUtil.requireAdmin();
        Claim claim = requirePending(claimId);
        LostItem item = itemMapper.selectById(claim.getItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }

        int itemRows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                .eq(LostItem::getId, item.getId())
                .eq(LostItem::getItemStatus, ItemStatusEnum.PROCESSING.getCode())
                .set(LostItem::getItemStatus, ItemStatusEnum.COMPLETED.getCode()));
        if (itemRows != 1) {                                                              // 并发或状态已变
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);
        }

        int claimRows = claimMapper.update(null, new LambdaUpdateWrapper<Claim>()
                .eq(Claim::getId, claimId)
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode())
                .set(Claim::getStatus, ClaimStatusEnum.APPROVED.getCode())
                .set(Claim::getAdminId, SecurityUtil.getUserId())
                .set(Claim::getReply, reply));
        if (claimRows != 1) {                                                             // 回滚物品状态
            throw new BusinessException(ResultCode.CLAIM_STATUS_ILLEGAL);
        }

        // 其余待核验申请自动拒绝并通知
        List<Claim> others = claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, item.getId())
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode())
                .ne(Claim::getId, claimId));
        for (Claim other : others) {
            claimMapper.update(null, new LambdaUpdateWrapper<Claim>()
                    .eq(Claim::getId, other.getId())
                    .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode())
                    .set(Claim::getStatus, ClaimStatusEnum.REJECTED.getCode())
                    .set(Claim::getReply, "该物品已被他人认领"));
            messageService.push(other.getApplicantId(), MessageTypeEnum.CLAIM_RESULT,
                    "你申请的「" + item.getTitle() + "」已被他人认领，申请自动取消", item.getId());
        }

        messageService.push(claim.getApplicantId(), MessageTypeEnum.CLAIM_RESULT,
                "你的认领申请已通过，请与发布者联系交接", item.getId());
        messageService.push(item.getUserId(), MessageTypeEnum.CLAIM_RESULT,
                "你的物品已完成归还", item.getId());
        return new ClaimResultVO(claimId, ItemStatusEnum.COMPLETED.getCode());
    }

    // ---------------- U4.4 核验拒绝 ----------------

    @Override
    @Transactional
    public ClaimResultVO reject(Long claimId, String reply) {
        SecurityUtil.requireAdmin();
        if (!StringUtils.hasText(reply) || reply.length() > 200) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写 1~200 字的拒绝原因");
        }
        Claim claim = requirePending(claimId);

        int rows = claimMapper.update(null, new LambdaUpdateWrapper<Claim>()
                .eq(Claim::getId, claimId)
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode())
                .set(Claim::getStatus, ClaimStatusEnum.REJECTED.getCode())
                .set(Claim::getAdminId, SecurityUtil.getUserId())
                .set(Claim::getReply, reply));
        if (rows != 1) {
            throw new BusinessException(ResultCode.CLAIM_STATUS_ILLEGAL);
        }

        // 没有其他待核验申请时，物品退回进行中
        long remaining = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, claim.getItemId())
                .eq(Claim::getStatus, ClaimStatusEnum.PENDING.getCode()));
        int newStatus = ItemStatusEnum.PROCESSING.getCode();
        if (remaining == 0) {
            int itemRows = itemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                    .eq(LostItem::getId, claim.getItemId())
                    .eq(LostItem::getItemStatus, ItemStatusEnum.PROCESSING.getCode())
                    .set(LostItem::getItemStatus, ItemStatusEnum.ONGOING.getCode()));
            if (itemRows == 1) {
                newStatus = ItemStatusEnum.ONGOING.getCode();
            }
        }

        messageService.push(claim.getApplicantId(), MessageTypeEnum.CLAIM_RESULT,
                "你的认领申请未通过：" + reply, claim.getItemId());
        return new ClaimResultVO(claimId, newStatus);
    }

    // ---------------- U4.5 核验列表 ----------------

    @Override
    public PageResult<ClaimAdminVO> page(ClaimQuery query) {
        SecurityUtil.requireAdmin();
        String keyword = query.trimmedKeyword();
        String like = keyword.isEmpty() ? null : "%" + escapeLike(keyword) + "%";
        IPage<ClaimAdminRow> page = claimMapper.search(new Page<>(query.safePage(), query.safeSize()),
                query.safeStatus(), like);
        List<ClaimAdminVO> records = page.getRecords().stream()
                .map(row -> new ClaimAdminVO(row.getId(), row.getItemId(), row.getItemTitle(),
                        row.getItemType(),
                        new ClaimAdminVO.Applicant(row.getApplicantId(), row.getApplicantNickname(),
                                row.getApplicantStudentNo()),
                        row.getDetailDesc(), row.getStatus(),
                        ClaimStatusEnum.textOf(row.getStatus()), row.getReply(), row.getCreatedAt()))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), records);
    }

    // ---------------- 内部工具 ----------------

    private Claim requirePending(Long claimId) {
        Claim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ItemStatusMachine.checkClaim(claim.getStatus());                                  // 3002
        return claim;
    }

    private String nicknameOf(Long uid) {
        User user = userMapper.selectById(uid);
        return user == null ? "用户" : user.getNickname();
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
