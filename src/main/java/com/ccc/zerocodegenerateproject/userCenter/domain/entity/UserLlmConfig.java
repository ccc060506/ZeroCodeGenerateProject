package com.ccc.zerocodegenerateproject.userCenter.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserLlmConfig {

    @TableId("user_id")
    private Long userId;
    private String provider;        // "doubao" / "kimi" / "deepseek" / "openai" / "gemini" / "grok"
    private String apiKey;          // 用户自己的 API Key
    private String baseUrl;
    private String modelName;       // 如 kimi-k2.5、deepseek-chat、gpt-4o 等
    private Double temperature = 0.5;
    private Integer maxTokens = 32768;
}
