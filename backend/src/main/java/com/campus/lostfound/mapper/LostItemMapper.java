package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.dto.AuditRow;
import com.campus.lostfound.dto.CategoryStatRow;
import com.campus.lostfound.dto.DailyCountRow;
import com.campus.lostfound.entity.LostItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 失物信息数据访问。 */
@Mapper
public interface LostItemMapper extends BaseMapper<LostItem> {

    /**
     * 关键词搜索（详细设计 4.3.2）：标题与描述模糊匹配，标题命中优先，再按发布时间倒序。
     */
    @Select("""
            <script>
            SELECT * FROM lost_item
            WHERE audit_status = 1 AND item_status IN (0, 1)
            <if test="type != null"> AND type = #{type} </if>
            <if test="categoryId != null"> AND category_id = #{categoryId} </if>
            AND (title LIKE #{like} OR description LIKE #{like})
            ORDER BY (CASE WHEN title LIKE #{like} THEN 0 ELSE 1 END) ASC, created_at DESC
            </script>
            """)
    IPage<LostItem> search(IPage<LostItem> page,
                           @Param("like") String like,
                           @Param("type") Integer type,
                           @Param("categoryId") Long categoryId);

    /** 待审核列表（API 规格 7.1）：物品 ⊕ 发布者 ⊕ 分类，已关闭信息不进入待审队列。 */
    @Select("""
            <script>
            SELECT i.id, i.title, i.type, i.category_id, i.description,
                   i.audit_status, i.item_status, i.created_at,
                   u.nickname AS publisher_nickname, c.name AS category_name
            FROM lost_item i
            JOIN user u ON u.id = i.user_id
            LEFT JOIN category c ON c.id = i.category_id
            WHERE i.audit_status = #{auditStatus} AND i.item_status &lt;&gt; 3
            <if test="kw != null and kw != ''">
              AND (i.title LIKE #{kw} OR u.nickname LIKE #{kw})
            </if>
            ORDER BY i.created_at DESC
            </script>
            """)
    IPage<AuditRow> pageForAudit(IPage<AuditRow> page,
                                 @Param("auditStatus") Integer auditStatus,
                                 @Param("kw") String kw);

    /** 分类维度统计（API 规格 7.9）：信息数与已完成数。 */
    @Select("""
            SELECT c.id AS category_id, c.name AS category_name,
                   COUNT(i.id) AS item_count,
                   COALESCE(SUM(CASE WHEN i.item_status = 2 THEN 1 ELSE 0 END), 0) AS completed_count
            FROM category c
            LEFT JOIN lost_item i ON i.category_id = c.id
            GROUP BY c.id, c.name
            ORDER BY c.sort
            """)
    List<CategoryStatRow> categoryStats();

    /** 近 N 天发布量（API 规格 7.9）。 */
    @Select("""
            SELECT DATE(created_at) AS day, COUNT(*) AS total
            FROM lost_item
            WHERE created_at >= #{start}
            GROUP BY DATE(created_at)
            """)
    List<DailyCountRow> dailyCounts(@Param("start") LocalDateTime start);
}
