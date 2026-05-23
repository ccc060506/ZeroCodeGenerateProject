package com.ccc.zerocodegenerateproject.codeGenerateModule.controller;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.dto.ModifyDSLDTO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ModifyCodeService;
import com.ccc.zerocodegenerateproject.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/modify")
public class ModifyCodeController {
    @Autowired
    private ModifyCodeService modifyCodeService;

    // 进入编辑页面
    // 修改组件----前端增量发送
    @PutMapping("/component/{projectId}/pageId/{pageId}/versionNum/{versionNum}")
    public Result<String> updateComponent(@PathVariable("projectId") Long projectId ,
                                          @PathVariable("pageId") long pageId,
                                          @PathVariable("versionNum") Integer versionNum,
                                          @RequestBody ModifyDSLDTO json ){
        log.info("已接收到前端修改 JSON");
        Boolean res = modifyCodeService.updateComponent(projectId ,pageId ,versionNum , json);
        if(res){
            return Result.success("修改成功");
        }
        return Result.error("修改失败,请重试");
    }

    // 保存修改
    @PostMapping("/component/save/{projectId}")
    public Result<String> saveModify(@PathVariable Long projectId){
        log.info("保存修改的组件");
        Boolean res = modifyCodeService.saveProject(projectId);
        if(res){
            return Result.success("保存成功");
        }
        return Result.error("保存失败,请重试");
    }
}
