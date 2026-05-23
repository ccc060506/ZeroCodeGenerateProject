package com.ccc.zerocodegenerateproject.projectManager.domain;

public enum GenerateTaskStatus {
    PENDING("PENDING", "待处理"),
    RUNNING("RUNNING", "生成中"),
    SUCCESS("SUCCESS", "生成成功"),
    FAILED("FAILED", "生成失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    GenerateTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
