package com.delmoralcristian.notifier.application.dto;

import java.io.Serializable;
import java.util.List;

public record PagedResponse<T extends Serializable>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) implements Serializable {

}
