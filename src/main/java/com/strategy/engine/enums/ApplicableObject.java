package com.strategy.engine.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 适用对象枚举
 */
@Getter
@AllArgsConstructor
public enum ApplicableObject {

    STUDENT("STUDENT", "学生"),
    CLASS("CLASS", "班级"),
    SCHOOL("SCHOOL", "学校"),
    BUREAU("BUREAU", "教育局");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    public static ApplicableObject getByCode(String code) {
        for (ApplicableObject obj : values()) {
            if (obj.getCode().equals(code)) {
                return obj;
            }
        }
        return null;
    }
}
