package com.ccc.zerocodegenerateproject.projectManager.domain.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectVO {

    private Long id;

    /**
     * 项目唯一编码
     */
    @TableField(value = "project_code")
    private String projectCode;

    // 项目名称
    private String projectName;

    // 项目类型
    private String projectType;

    /** 项目首页预览图 URL 或存储路径 */
    private String previewImage;

    /**
     * 项目描述（AI 根据用户 prompt 生成）
     */
    @TableField(value = "description")
    private String description;

    /**
     * CodeDSL JSON字符串（整个 CodeDSL 对象序列化后的结果）
     */
    private String CodeDSLJson;

    // 技术栈
    private String techStack;

    // 创建时间
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
