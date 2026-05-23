package com.ccc.zerocodegenerateproject.userCenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.common.exception.BusinessException;
import com.ccc.zerocodegenerateproject.common.util.SHA256;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserInfoDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.dto.UserRegisterDTO;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import com.ccc.zerocodegenerateproject.userCenter.domain.vo.UserVO;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import com.ccc.zerocodegenerateproject.userCenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    // 注册接口
    public Boolean register(UserRegisterDTO userRegisterDTO){
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, userRegisterDTO.getUsername());
        if (baseMapper.selectCount(query) > 0) {
            throw new RuntimeException("该用户名已被注册");
        }

        // 密码加密
        String salt = SHA256.getSalt();
        String password = SHA256.encode(userRegisterDTO.getPassword(),salt);
        User user = new User();
        user.setUsername(userRegisterDTO.getUsername());
        user.setSalt(salt);
        user.setPassword(password);
        user.setCreateTime(LocalDateTime.now());
        int res = userMapper.insert(user);
        return res == 1;
    }

    // 查询个人信息
    public UserVO getUserInfo(){
        Long id = UserContext.getId();
        if(id==null){
            throw new BusinessException(404, "用户不存在或已删除");
        }
        User user = userMapper.selectById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    // 修改个人信息
    public Boolean updateUserInfo(UserInfoDTO userInfoDTO){
        if(userInfoDTO.getUsername()==null||userInfoDTO.getEmail()==null)
        {
            throw new BusinessException(500,"个人信息填写不能为空");
        }
        int updateCount = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, UserContext.getId())
                .set(User::getAvatar, userInfoDTO.getAvatar())
                .set(User::getUsername, userInfoDTO.getUsername())
                .set(User::getEmail, userInfoDTO.getEmail())
        );
        return updateCount!=0;
    }

    public void updateAvatar(String base64Avatar) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
            .eq(User::getId, UserContext.getId())
            .set(User::getAvatar, base64Avatar)
        );
    }

    // 更新用户 LLM 配置信息
    public Boolean updateLlmConfig(String provider, String apiKey) {

        Long id = UserContext.getId();
        UserLlmConfig userLlmConfig = new UserLlmConfig();
        userLlmConfig.setUserId(id);
        userLlmConfig.setProvider(provider);
        userLlmConfig.setApiKey(apiKey);
        userLlmConfig.setTemperature(0.3);
        userLlmConfig.setMaxTokens(32768);

        log.info(provider);
        log.info(apiKey);
        switch (provider){
            case "doubao":
                userLlmConfig.setBaseUrl("https://ark.cn-beijing.volces.com/api/v3");
                userLlmConfig.setModelName("doubao-seed-2-0-code");
                break;
            case "kimi":
                userLlmConfig.setBaseUrl("https://api.moonshot.cn/v1");
                userLlmConfig.setModelName("kimi-k2.5");
                break;
            case "deepseek":
                userLlmConfig.setBaseUrl("https://api.deepseek.com");
                userLlmConfig.setModelName("deepseek-chat");
                break;
            case "openai":
                userLlmConfig.setBaseUrl("https://api.openai.com/v1");
                userLlmConfig.setModelName("gpt-4o");
                break;
            case "gemini":   // Google Gemini（OpenAI 兼容模式）
                userLlmConfig.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai/");
                userLlmConfig.setModelName("gemini-2.5-pro");
                break;
            case "grok":
                userLlmConfig.setBaseUrl("https://api.x.ai/v1");
                userLlmConfig.setModelName("grok-4.20");
                break;
            default:
                throw new IllegalArgumentException("不支持的 LLM 平台: " + provider);
        }

        // 将模型存入缓存和数据库
        String llmCache = "user:llm:id" + id;
        redisTemplate.opsForValue().set(llmCache , userLlmConfig ,7 , TimeUnit.DAYS);
        User user = userMapper.selectById(id);

        // 查询是否已经有配置
        if(userMapper.selectUserLlmConfig(UserContext.getId())==null){
            // 没有则新增
            user.setUserLlmConfig(userLlmConfig);
            int res = userMapper.insertUserLlmConfig(userLlmConfig);
            return res == 1;
        } else {
            // 有则修改
            int res = userMapper.updateLlmConfig(id, userLlmConfig);
            return res == 1;
        }
    }

    // 获取当前使用的 apiKey
    public String getApiKey(){
        Long id = UserContext.getId();
        if (id==null){
            throw new BusinessException(404, "用户不存在或已删除");
        }
        String apiKey = userMapper.selectApiKey(id);
        return apiKey.substring(0, 8) + "xxxx-xxxx-xxxx";
    }
}
