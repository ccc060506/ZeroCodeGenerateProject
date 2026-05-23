package com.ccc.zerocodegenerateproject.projectManager.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ccc.zerocodegenerateproject.projectManager.domain.GenerateTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 项目生成任务实体类
 * 用于记录 AI 异步生成项目的任务状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_generate_task")
public class ProjectGenerateTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一编码（推荐格式：TASK_20260415123456789）
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 关联的项目ID（生成成功后回填）
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户输入的原始提示词
     */
    @TableField("gen_prompt")
    private String genPrompt;

    /**
     * 任务状态
     */
    @TableField("status")
    private String status;   // 使用枚举 GenerateTaskStatus

    /**
     * 生成进度（0-100）
     */
    @TableField("progress")
    private Integer progress;

    /**
     * AI 推断出的项目名称
     */
    @TableField("ai_project_name")
    private String aiProjectName;

    /**
     * AI 推断出的项目描述
     */
    @TableField("ai_description")
    private String aiDescription;

    /**
     * AI 推断出的项目类型
     */
    @TableField("ai_project_type")
    private String aiProjectType;

    /**
     * AI 推断出的技术栈
     */
    @TableField("ai_tech_stack")
    private String aiTechStack;

    /**
     * 错误信息（失败时记录）
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 开始生成时间
     */
    @TableField("generate_time")
    private LocalDateTime generateTime;

    /**
     * 完成时间
     */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
