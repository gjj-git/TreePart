package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 场景匹配结果 VO
 */
@Data
@Schema(description = "场景匹配结果")
public class SceneMatchVO {

    @Schema(description = "是否有命中场景")
    private Boolean matched;

    @Schema(description = "命中的场景列表")
    private List<MatchedScene> matchedScenes;

    @Data
    @Schema(description = "命中场景信息")
    public static class MatchedScene {

        @Schema(description = "场景ID")
        private Long sceneId;

        @Schema(description = "场景名称")
        private String sceneName;

        @Schema(description = "场景描述")
        private String description;

        @Schema(description = "命中的标签规则数量")
        private Integer matchedTagCount;
    }
}
