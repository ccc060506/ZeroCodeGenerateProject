package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.dto;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageSaveDTO {

    private Long id;                    // 更新时传入
    private Long projectId;
    private String pageName;
    private String title;
    private String route;
    private String status;              // draft / published
    private PageDSL dsl;                // 注意：这里用你修改后的 PageDSL
}
