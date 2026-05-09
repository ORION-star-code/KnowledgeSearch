package com.knowledge.search.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArticleStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    OFFLINE("offline");

    @EnumValue
    @JsonValue
    private final String value;
}
