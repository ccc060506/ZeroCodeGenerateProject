package com.ccc.zerocodegenerateproject.codeGenerateModule.controller;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.ComponentVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ComponentCodeService;
import com.ccc.zerocodegenerateproject.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/component")
public class ComponentCodeController {
    @Autowired
    private ComponentCodeService componentCodeService;

    /**
     * 根据 项目Id 和 组件Id 查看组件详情
     * @param componentId
     * @return
     */
    // 预览组件效果 Component
    @GetMapping("/{projectId}/{componentId}")
    public Result<ComponentVO> getComponentDetail(
            @PathVariable("/projectId") long projectId,
            @PathVariable("/componentId") long componentId){
        log.info("查看项目id为{} , 组件id为 {} 的组件详情",projectId,componentId);
        ComponentVO res = componentCodeService.getComponentDetail(projectId,componentId);
        return Result.success(res);
    }
}
