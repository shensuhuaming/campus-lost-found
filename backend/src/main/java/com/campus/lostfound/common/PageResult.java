package com.campus.lostfound.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应（API 规格 1.3）：{total, page, size, pages, records}。
 */
public record PageResult<T>(long total, int page, int size, int pages, List<T> records) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), page.getRecords());
    }

    /** 分页对象转换：entity 页 → VO 页。 */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<List<E>, List<V>> converter) {
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), converter.apply(page.getRecords()));
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(0, page, size, 0, List.of());
    }
}
