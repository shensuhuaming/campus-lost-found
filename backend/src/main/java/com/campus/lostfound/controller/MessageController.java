package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.MessageQuery;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.MessageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 站内消息接口（API 规格 6.3 ~ 6.5）。 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 我的消息列表。 */
    @GetMapping
    public Result<PageResult<MessageVO>> page(MessageQuery query) {
        return Result.ok(messageService.page(query));
    }

    /** 标记单条已读。 */
    @PutMapping("/{id}/read")
    public Result<Map<String, Object>> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.ok(Map.of("id", id, "isRead", 1));
    }

    /** 未读消息数（导航角标，前端 60 秒轮询）。 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", messageService.unreadCount()));
    }
}
