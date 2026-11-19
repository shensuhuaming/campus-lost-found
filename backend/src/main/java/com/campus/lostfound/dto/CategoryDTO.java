package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 分类新增/修改请求（API 规格 7.4、7.5）。 */
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 20, message = "分类名称不超过 20 字")
    private String name;

    private Integer sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
