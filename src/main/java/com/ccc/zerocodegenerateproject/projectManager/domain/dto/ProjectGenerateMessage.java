package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectGenerateMessage implements Serializable {

    private String taskCode;        // 任务唯一编码
    private Long userId;
    private String genPrompt;       // 原始 prompt
}
