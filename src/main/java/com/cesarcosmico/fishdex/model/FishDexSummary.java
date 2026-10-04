package com.cesarcosmico.fishdex.model;

import java.util.Map;

public record FishDexSummary(Map<String, CategoryProgress> perCategory, CategoryProgress overall) {

    public CategoryProgress category(String id) {
        return perCategory.getOrDefault(id, new CategoryProgress(0, 0, 0, 0));
    }
}
