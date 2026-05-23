package com.ccc.zerocodegenerateproject.userCenter.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    private String username;
    private String avatar;   // 头像地址
    private String email;
    private String apiKey;
}
