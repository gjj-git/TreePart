package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 标签使用情况 VO
 */
@Data
@Schema(description = "标签使用情况响应对象")
public class TagUsageVO {

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "使用该标签的场景数量")
    private Long sceneCount;

    @Schema(description = "使用该标签的场景列表")
    private List<SceneInfo> scenes;

    @Data
    @Schema(description = "场景基本信息")
    public static class SceneInfo {
        @Schema(description = "场景ID")
        private Long sceneId;

        @Schema(description = "场景名称")
        private String sceneName;
    }
}
