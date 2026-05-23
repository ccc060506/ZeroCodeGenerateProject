package com.ccc.zerocodegenerateproject.userCenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.common.util.SHA256;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.ResetPasswordDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.VerifyCodeDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import com.ccc.zerocodegenerateproject.userCenter.service.PasswordService;
import com.ccc.zerocodegenerateproject.common.util.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl extends ServiceImpl<UserMapper, User> implements PasswordService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    private final MailService mailService;

    // 发送验证码
    public int sendVerifyCode(String email){
        String subject = "【密码找回】验证码通知";
        String verifyCode = generateVerifyCode();
        String content = "您好，您正在申请密码找回，您的验证码是：" + verifyCode + "，有效期5分钟，请勿泄露给他人。";
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if(user!=null){
            mailService.sendSimpleMail(email,subject,content);
            redisTemplate.opsForValue().set("verify:code:"+ email , verifyCode ,5, TimeUnit.MINUTES);
            return 1;
        }
        return 0;
    }

    // 验证 验证码
    public int ensureCode(VerifyCodeDTO verifyCodeDTO){
        Object obj = redisTemplate.opsForValue().get("verify:code:" + verifyCodeDTO.getEmail());
        if(obj==null){
            return -1;
        }
        String code = obj.toString();
        if(code.equals(verifyCodeDTO.getCode())){
            return 1;
        }
        return 0;
    }

    // 重置密码
    public int resetPassword(ResetPasswordDTO resetPasswordDTO){

        String email = resetPasswordDTO.getEmail();
        String newPassword = resetPasswordDTO.getNewPassword();
        String code = resetPasswordDTO.getCode();

        // 验证验证码是否正确
        Object obj = redisTemplate.opsForValue().get("verify:code:" + email);
        if(obj==null){
            return 0;
        }
        String redisCode = obj.toString();
        if(!redisCode.equals(code)){
            return 0;
        }

        // 验证用户是否存在
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            return -1;
        }

        // 重置盐值和密码
        String newSalt = SHA256.getSalt();
        String encodedPassword = SHA256.encode(newPassword, newSalt);
        user.setPassword(encodedPassword);
        user.setSalt(newSalt);
        user.setUpdateTime(LocalDateTime.now());
        int rows = userMapper.updateById(user);
        redisTemplate.delete("verify:code:" + email);
        return rows > 0 ? 1 : 0;
    }

    // 生成验证码
    public String generateVerifyCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
