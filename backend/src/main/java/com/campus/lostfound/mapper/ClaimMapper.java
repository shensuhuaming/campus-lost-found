package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.dto.ClaimAdminRow;
import com.campus.lostfound.entity.Claim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 认领申请数据访问。 */
@Mapper
public interface ClaimMapper extends BaseMapper<Claim> {

    /**
     * 管理员核验列表（详细设计 4.4.5）：认领 ⊕ 物品 ⊕ 申请人三表关联。
     * keyword 已在 Service 中拼成 LIKE 模式并转义，按标题或学号匹配。
     */
    @Select("""
            <script>
            SELECT c.id, c.item_id, i.title AS item_title, i.type AS item_type,
                   u.id AS applicant_id, u.nickname AS applicant_nickname,
                   u.student_no AS applicant_student_no,
                   c.detail_desc, c.status, c.reply, c.created_at
            FROM claim c
            JOIN lost_item i ON i.id = c.item_id
            JOIN user u ON u.id = c.applicant_id
            WHERE c.status = #{status}
            <if test="kw != null and kw != ''">
              AND (i.title LIKE #{kw} OR u.student_no LIKE #{kw})
            </if>
            ORDER BY c.created_at ASC
            </script>
            """)
    IPage<ClaimAdminRow> search(IPage<ClaimAdminRow> page,
                                @Param("status") Integer status,
                                @Param("kw") String kw);
}
