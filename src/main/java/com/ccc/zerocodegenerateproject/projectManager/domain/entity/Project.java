package com.ccc.zerocodegenerateproject.projectManager.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目唯一编码
     */
    @TableField(value = "project_code")
    private String projectCode;

    /**
     * 项目名称
     */
    @TableField(value = "project_name")
    private String projectName;

    /**
     * 项目描述（AI 根据用户 prompt 生成）
     */
    @TableField(value = "description")
    private String description;

    /**
     * 用户输入的原始提示词（后续AI上下文）
     */
    @TableField(value = "gen_prompt")
    private String genPrompt;

    /**
     * CodeDSL JSON字符串（整个 CodeDSL 对象序列化后的结果）
     */
    @TableField(value = "code_dsl_json")
    private String CodeDSLJson;

    /**
     * 项目类型（如：CRM、OA、管理后台、H5 等）
     */
    @TableField(value = "project_type")
    private String projectType;


    /** 项目首页预览图 URL 或存储路径 */
    @TableField(value = "preview_image")
    private String previewImage;

    /**
     * 浏览量（总计）
     */
    @TableField(value = "view_count")
    private Long viewCount = 0L;

    /**
     * 点赞数（总计）
     */
    @TableField(value = "like_count")
    private Long likeCount = 0L;

    /**
     * 技术栈偏好（如：react_ant、vue_element 等）
     */
    @TableField(value = "tech_stack")
    private String techStack;

    /**
     * 创建人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 公开：0-私人，1-公开
     */
    @TableField(value = "is_public")
    private Integer isPublic = 0;

    /**
     * 生成状态
     */
    @TableField(value = "status")
    private String status;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
