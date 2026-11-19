package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.entity.LostItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 失物信息数据访问。 */
@Mapper
public interface LostItemMapper extends BaseMapper<LostItem> {

    /**
     * 关键词搜索（详细设计 4.3.2）：标题与描述模糊匹配，标题命中优先，再按发布时间倒序。
     * <p>
     * 关键词已在 Service 中转义 % _ \，这里只负责拼装与排序；分页由 MyBatis-Plus 插件处理。
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
}
