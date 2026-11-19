package com.campus.lostfound.dto;

import jakarta.validation.constraints.Size;

/** 核验回复/拒绝理由（API 规格 5.3、5.4）：通过时可选，拒绝时必填，≤200 字。 */
public class ClaimReplyDTO {

    @Size(max = 200, message = "回复不超过 200 字")
    private String reply;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
