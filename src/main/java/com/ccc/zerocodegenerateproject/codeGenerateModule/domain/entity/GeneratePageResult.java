package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;


public record GeneratePageResult(
        CodeDSL dsl,                    // 结构化 DSL
        String pageGeneratedCode        // 完整可运行代码
) {
    public CharSequence getPageGeneratedCode() {
        return this.pageGeneratedCode;
    }
}
