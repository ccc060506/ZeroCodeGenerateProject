package com.ccc.zerocodegenerateproject.userCenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserInfoDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserRegisterDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.VerifyCodeDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.domain.vo.UserVO;

public interface UserService extends IService<User> {

    Boolean register(UserRegisterDTO userRegisterDTO);

    Boolean updateLlmConfig(String provider , String apiKey);

    String getApiKey();

    UserVO getUserInfo();

    Boolean updateUserInfo(UserInfoDTO userInfoDTO);
    void updateAvatar(String base64Avatar);
}
