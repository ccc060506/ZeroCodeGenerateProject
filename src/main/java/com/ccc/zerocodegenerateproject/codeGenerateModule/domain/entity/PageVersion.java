package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PageVersion {

    private Long id;
    @TableField(value = "page_dsl_id")
    private Long pageDSLId;         // 关联主 Page
    private Integer versionNum;     // 1,2,3,... 自动递增
    private String versionRemark;   // 可选：v1.0、20260418-修改登录页
    private String dslJson;         // 完整 PageDSL 的 JSON 快照
    private String remark;          // 本次修改说明
    private String operator;        // 操作人
    private LocalDateTime createTime;
    private String status;          // DRAFT / PUBLISHED / ARCHIVED
    // 存储 AI 生成的原始代码
    private String PageGeneratedCode;
    // 用户修改后的最终代码（推荐）
    private String userEditedPageCode;
}
