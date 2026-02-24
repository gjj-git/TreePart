package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 策略引擎实体
 */
@Data
@TableName("strategy_engine")
public class StrategyEngine {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 引擎名称
     */
    private String name;

    /**
     * 引擎类型：COMPREHENSIVE_REVIEW-综合复习, SINGLE_EXAM-单场考试
     */
    private String type;

    /**
     * 适用对象：STUDENT-学生, CLASS-班级, GRADE-年级, BUREAU-教育局
     */
    private String applicableObject;

    /**
     * 引擎描述
     */
    private String description;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 是否默认：0-否, 1-是
     */
    private Integer isDefault;

    /**
     * 标签总数
     */
    private Integer tagCount;

    /**
     * 策略总数
     */
    private Integer sceneCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
