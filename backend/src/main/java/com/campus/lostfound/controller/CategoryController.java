package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公开分类接口（API 规格 4.3）：供首页筛选与发布页下拉使用，不含统计。 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public Result<List<CategoryVO>> list() {
        List<CategoryVO> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort))
                .stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getSort()))
                .toList();
        return Result.ok(list);
    }
}
