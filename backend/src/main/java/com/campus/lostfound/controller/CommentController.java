package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.CommentCreateDTO;
import com.campus.lostfound.service.CommentService;
import com.campus.lostfound.vo.CommentCreatedVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 留言接口（API 规格 6.1、6.2）。 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 发表留言（U5.1）。 */
    @PostMapping
    public Result<CommentCreatedVO> create(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.ok(commentService.create(dto));
    }

    /** 删除留言（U5.2）：留言本人或管理员。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}
