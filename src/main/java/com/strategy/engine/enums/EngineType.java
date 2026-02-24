package com.strategy.engine.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 引擎类型枚举
 */
@Getter
@AllArgsConstructor
public enum EngineType {

    COMPREHENSIVE_REVIEW("COMPREHENSIVE_REVIEW", "综合复习"),
    SINGLE_EXAM("SINGLE_EXAM", "单场考试");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    public static EngineType getByCode(String code) {
        for (EngineType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
