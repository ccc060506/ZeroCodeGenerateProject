package com.ccc.zerocodegenerateproject.projectManager.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRankVO {

    private Long id;

    // 发布此项目用户的头像
    private String avatar;

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

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
}
