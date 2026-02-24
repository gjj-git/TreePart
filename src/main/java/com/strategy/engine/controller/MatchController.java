package com.strategy.engine.controller;

import com.strategy.engine.common.Result;
import com.strategy.engine.dto.SceneMatchDTO;
import com.strategy.engine.service.MatchService;
import com.strategy.engine.vo.SceneMatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场景匹配 Controller
 */
@Tag(name = "场景匹配")
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "场景匹配", description = "根据输入数据匹配引擎下满足所有启用标签规则的场景")
    @PostMapping
    public Result<SceneMatchVO> match(@Validated @RequestBody SceneMatchDTO dto) {
        SceneMatchVO vo = matchService.match(dto);
        return Result.success(vo);
    }
}
