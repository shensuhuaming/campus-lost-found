package com.campus.lostfound.service;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.ClaimCreateDTO;
import com.campus.lostfound.dto.ClaimQuery;
import com.campus.lostfound.vo.ClaimAdminVO;
import com.campus.lostfound.vo.ClaimResultVO;

/** 认领服务（详细设计 4.4）：发起、核验通过、核验拒绝、待核验列表。 */
public interface ClaimService {

    /** 发起认领（U4.2）：物品进入处理中，通知发布者。 */
    ClaimResultVO create(ClaimCreateDTO dto);

    /** 核验通过（U4.3）：物品置已完成，其余待核验申请自动拒绝。 */
    ClaimResultVO approve(Long claimId, String reply);

    /** 核验拒绝（U4.4）：无其他待核验申请时物品退回进行中。 */
    ClaimResultVO reject(Long claimId, String reply);

    /** 待核验列表（U4.5）：管理员，默认查待核验。 */
    PageResult<ClaimAdminVO> page(ClaimQuery query);
}
