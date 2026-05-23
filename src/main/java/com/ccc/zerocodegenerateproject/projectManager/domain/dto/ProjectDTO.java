package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDTO {

    /**
     * 用户输入的原始提示词
     * 示例："我要做一个企业内部的CRM系统，主要管理客户、销售机会和合同"
     */
    @NotBlank(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词长度不能超过2000个字符")
    private String prompt;
}
