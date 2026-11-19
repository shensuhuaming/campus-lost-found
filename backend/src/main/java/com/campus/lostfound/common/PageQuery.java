package com.campus.lostfound.common;

/**
 * 统一分页请求参数（API 规格 1.3）：page 从 1 开始，size 默认 12、最大 50。
 * 越界一律纠正而不是报错，避免前端传错参数导致 500。
 */
public class PageQuery {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    private Integer page;
    private Integer size;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safeSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
