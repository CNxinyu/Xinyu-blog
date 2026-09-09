package com.xinyu.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, long page, long size, long total, long totalPages) {

    public static <S, T> PageResponse<T> from(IPage<S> source, Function<S, T> mapper) {
        List<T> items = source.getRecords().stream().map(mapper).toList();
        long totalPages = source.getSize() == 0 ? 0 :
                (source.getTotal() + source.getSize() - 1) / source.getSize();
        return new PageResponse<>(items, source.getCurrent(), source.getSize(), source.getTotal(), totalPages);
    }
}
