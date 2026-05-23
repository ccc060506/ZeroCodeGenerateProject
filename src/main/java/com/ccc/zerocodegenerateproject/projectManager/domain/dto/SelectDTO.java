package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectDTO {

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
     * 项目类型（如：CRM、OA、管理后台、H5 等）
     */
    @TableField(value = "project_type")
    private String projectType;

    @TableField(value = "tech_stack")
    private String techStack;

    /**
     * 公开：0-私人，1-公开
     */
    @TableField(value = "isPublic")
    private Integer isPublic;

    // 滚动分页参数
    private Long lastId;        // 上次返回的最后一条记录的 id
    private LocalDateTime lastCreateTime; // 防止同一时间创建的多条记录漏掉
    private Integer size = 20;  // 每页大小，默认20条
}
