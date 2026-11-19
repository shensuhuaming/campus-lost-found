package com.campus.lostfound.service;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.dto.ItemSaveDTO;
import com.campus.lostfound.vo.CategoryVO;
import com.campus.lostfound.vo.ItemCardVO;
import com.campus.lostfound.vo.ItemDetailVO;

import java.util.List;

/** 失物信息与浏览检索服务（详细设计 4.2、4.3）。 */
public interface ItemService {

    /** 发布（U2.1）：进入待审核，返回新信息 id。 */
    Long create(ItemSaveDTO dto);

    /** 编辑（U2.3）：仅进行中可编辑，编辑后回到待审核。 */
    void update(Long id, ItemSaveDTO dto);

    /** 关闭（U2.4）：进行中/处理中可关闭，管理员必须填理由。 */
    void close(Long id, String reason);

    /** 寻物状态推进（U2.5）：0→1（已联系）、1→2（已找回）。 */
    void advance(Long id, Integer targetStatus);

    /** 列表与检索（U3.1、U3.2）。 */
    PageResult<ItemCardVO> page(ItemQuery query);

    /** 详情（U3.3）：联系方式按登录态掩码，含留言与我的认领状态。 */
    ItemDetailVO detail(Long id);

    /** 公开分类列表（API 4.3）。 */
    List<CategoryVO> categories();
}
