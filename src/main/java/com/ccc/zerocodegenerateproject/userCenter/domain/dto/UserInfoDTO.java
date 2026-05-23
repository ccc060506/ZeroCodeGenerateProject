package com.ccc.zerocodegenerateproject.userCenter.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {

    private String username;
    private String avatar;   // 头像地址
    private String email;
}
