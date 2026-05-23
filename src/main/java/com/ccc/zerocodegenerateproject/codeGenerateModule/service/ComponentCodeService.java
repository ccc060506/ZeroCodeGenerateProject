package com.ccc.zerocodegenerateproject.codeGenerateModule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.ComponentVO;

public interface ComponentCodeService extends IService<CodeDSL> {

    ComponentVO getComponentDetail(long projectId , long componentId);

}
