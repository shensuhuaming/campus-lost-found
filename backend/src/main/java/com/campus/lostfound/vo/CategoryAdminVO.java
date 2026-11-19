package com.campus.lostfound.vo;

/** 管理端分类（API 规格 7.3）：带该分类下的信息数。 */
public record CategoryAdminVO(Long id, String name, Integer sort, Long itemCount) {
}
