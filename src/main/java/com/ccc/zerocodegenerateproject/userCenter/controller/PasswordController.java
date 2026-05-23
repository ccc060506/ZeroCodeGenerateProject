package com.ccc.zerocodegenerateproject.userCenter.controller;

import com.ccc.zerocodegenerateproject.common.result.Result;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.ForgotPasswordRequestDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.ResetPasswordDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.VerifyCodeDTO;
import com.ccc.zerocodegenerateproject.userCenter.service.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user/password")
public class PasswordController {

    @Autowired
    private PasswordService passwordService;

    // 发送验证码接口
    @PostMapping("/sendCode")
    public Result<String> sendVerifyCode(@RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO){
        log.info("开始向邮箱为{}发送验证码",forgotPasswordRequestDTO.getEmail());
        int i = passwordService.sendVerifyCode(forgotPasswordRequestDTO.getEmail());
        if(i!=0){
            return Result.success("验证码已发送，请注意查收");
        }else {
            return Result.error("验证码发送失败");
        }
    }

    // 验证 验证码接口
    @PostMapping("/ensureCode")
    public Result<String> ensureCode(@RequestBody VerifyCodeDTO verifyCodeDTO){
        log.info("开始验证 验证码");
        int res = passwordService.ensureCode(verifyCodeDTO);
        if(res != 0&& res != -1){
            return Result.success("验证正确");
        }else if(res == 0){
            return Result.error("验证码错误或已过期");
        }else {
            return Result.error("当前用户不存在,请注册");
        }
    }

    // 重置密码
    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO){
        log.info("执行密码重置");
        int resetResult = passwordService.resetPassword(resetPasswordDTO);
        if (resetResult != 0 && resetResult != -1) {
            return Result.success("密码重置成功");
        } else if(resetResult == 0){
            return Result.error("密码重置失败");
        } else {
            return Result.error("用户不存在,请注册");
        }
    }
}
