package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.CloseDTO;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.dto.ItemSaveDTO;
import com.campus.lostfound.dto.ItemStatusDTO;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.vo.CategoryVO;
import com.campus.lostfound.vo.ItemCardVO;
import com.campus.lostfound.vo.ItemDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 失物信息与浏览检索接口（API 规格 3.1~3.3、3.5、4.1~4.3）。 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /** 发布信息（U2.1）。 */
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ItemSaveDTO dto) {
        Long id = itemService.create(dto);
        return Result.ok(Map.of("id", id));
    }

    /** 编辑信息（U2.3）：仅进行中可编辑，编辑后回到待审核。 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ItemSaveDTO dto) {
        itemService.update(id, dto);
        return Result.ok();
    }

    /** 关闭信息（U2.4）：管理员必须填理由。 */
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id, @RequestBody(required = false) CloseDTO dto) {
        itemService.close(id, dto == null ? null : dto.getReason());
        return Result.ok();
    }

    /** 寻物状态推进（U2.5）：1 处理中，2 已完成。 */
    @PutMapping("/{id}/status")
    public Result<Void> advance(@PathVariable Long id, @Valid @RequestBody ItemStatusDTO dto) {
        itemService.advance(id, dto.getTargetStatus());
        return Result.ok();
    }

    /** 列表与检索（U3.1、U3.2）：仅返回已通过审核且未关闭的信息。 */
    @GetMapping
    public Result<PageResult<ItemCardVO>> page(ItemQuery query) {
        return Result.ok(itemService.page(query));
    }

    /** 详情（U3.3）。 */
    @GetMapping("/{id}")
    public Result<ItemDetailVO> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id));
    }

    /** 公开分类列表（API 4.3）。 */
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(itemService.categories());
    }
}
