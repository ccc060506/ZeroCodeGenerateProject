package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVersionVO {

    private Integer versionNum;     // 1,2,3,... 自动递增
    private String versionRemark;   // 可选：v1.0、20260418-修改登录页
    private String dslJson;         // 完整 PageDSL 的 JSON 快照
    private String remark;          // 本次修改说明
    private String operator;        // 操作人
    private LocalDateTime createTime;
    private String status;          // DRAFT / PUBLISHED / ARCHIVED
}
