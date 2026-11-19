package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 物品图片（对应 lost_found.item_image），每条信息最多 3 张，sort 从 0 开始。 */
@Data
@TableName("item_image")
public class ItemImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    /** 相对路径，如 /uploads/202611/uuid.png */
    private String url;

    private Integer sort;
}
