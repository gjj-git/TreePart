package com.strategy.engine.service;

import com.strategy.engine.dto.SceneMatchDTO;
import com.strategy.engine.vo.SceneMatchVO;

/**
 * 场景匹配 Service
 */
public interface MatchService {

    /**
     * 根据输入数据匹配引擎下的场景
     *
     * @param dto 匹配请求（引擎ID + 待匹配数据）
     * @return 匹配结果
     */
    SceneMatchVO match(SceneMatchDTO dto);
}
