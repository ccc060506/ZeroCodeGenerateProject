package com.ccc.zerocodegenerateproject.userCenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.ResetPasswordDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.VerifyCodeDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;

public interface PasswordService extends IService<User> {

    int sendVerifyCode(String email);

    int ensureCode(VerifyCodeDTO verifyCodeDTO);

    int resetPassword(ResetPasswordDTO resetPasswordDTO);
}
