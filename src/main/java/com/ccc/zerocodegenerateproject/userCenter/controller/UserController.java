package com.ccc.zerocodegenerateproject.userCenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccc.zerocodegenerateproject.common.result.Result;
import com.ccc.zerocodegenerateproject.common.util.JwtUtil;
import com.ccc.zerocodegenerateproject.common.util.OssUtil;
import com.ccc.zerocodegenerateproject.common.util.SHA256;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserInfoDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserRegisterDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.VerifyCodeDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.LoginDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.vo.UserVO;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import com.ccc.zerocodegenerateproject.userCenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    // 登录接口
    @PostMapping("/login")
    public Result<Map<String,String>> login(@RequestBody LoginDTO loginDTO){
        log.info("进行登录");
        String account = loginDTO.getAccount().trim();
        String password = loginDTO.getPassword();
        User user = null;
        if (account.contains("@") && account.contains(".")) {
            // 邮箱登录
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, account));
        } else {
            // 用户名登录
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, account));
        }

        if (user == null) {
            return Result.error(401, "用户名/邮箱或密码错误");
        }
        if (!SHA256.encode(password, user.getSalt()).equals(user.getPassword())) {
            return Result.error(401, "用户名/邮箱或密码错误");
        }
        // 生成token
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        // token存入redis
        String redisKey = "token:" + token;
        redisTemplate.opsForValue().set(redisKey , user.getId(), 1, TimeUnit.DAYS);
        // 返回前端
        return Result.success(Map.of("token", token,
                "username", user.getUsername(),
                "email", user.getEmail()!=null ? user.getEmail() : "")
        );
    }

    // 注册接口
    @PostMapping("/register")
    public Result<String> register(@RequestBody UserRegisterDTO userRegisterDTO){
        log.info("进行注册");
        Boolean res = userService.register(userRegisterDTO);
        if(res){
            return Result.success("注册成功");
        }else {
            return Result.error("注册失败");
        }
    }

    // 查询个人信息
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(){
        log.info("个人信息查询");
        UserVO userVO = userService.getUserInfo();
        return Result.success(userVO);
    }

    // 修改个人信息
    @PutMapping("/update")
    public Result<String> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO){
        log.info("修改个人信息");
        Boolean res = userService.updateUserInfo(userInfoDTO);
        if(res){
            return Result.success("修改个人信息成功");
        }
        return Result.error("修改个人信息失败");
    }

    // 配置 api 接口
    @PostMapping("/llmConfig/update")
    public Result<String> updateLlmConfig(
            @RequestParam(value = "provider") String provider ,
            @RequestParam(value = "apiKey") String apiKey
    ){
        log.info("配置 api 接口");
        Boolean res = userService.updateLlmConfig(provider , apiKey);
        if(res){
            return Result.success("修改 Api 成功");
        }else {
            return Result.error("修改 Api 失败");
        }
    }

    // 查询当前使用的Api
    @GetMapping("/config/apiKey/select")
    public Result<String> getApiKey(){
        log.info("查询当前使用的Api");
        String api = userService.getApiKey();
        return Result.success(api);
    }

    @GetMapping("/config/provider/select")
    public Result<String> getProvider(){
        log.info("查看当前模型");
        String provider = userMapper.getProvider(UserContext.getId());
        return Result.success(provider);
    }

    @Autowired
    private OssUtil ossUtil;

    // 上传头像到 OSS
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件为空");
        try {
            String url = ossUtil.upload(file);
            userService.updateAvatar(url);
            return Result.success(url);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("上传失败");
        }
    }
}
