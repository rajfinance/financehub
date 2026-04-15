package com.financehub.dtos;

import lombok.Data;

import java.util.List;

@Data
public class CategoryReorderRequest {
    private List<Integer> orderedIds;
}
