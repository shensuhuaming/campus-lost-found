package com.campus.lostfound.dto;

import com.campus.lostfound.common.PageQuery;

/** 消息列表查询（API 规格 6.3）。 */
public class MessageQuery extends PageQuery {

    /** 0 未读，1 已读，不传为全部 */
    private Integer isRead;
    private Integer type;

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
