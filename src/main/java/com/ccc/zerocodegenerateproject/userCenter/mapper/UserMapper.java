package com.ccc.zerocodegenerateproject.userCenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    int updateLlmConfig(Long id, UserLlmConfig userLlmConfig);

    UserLlmConfig selectUserLlmConfig(Long userId);

    int insertUserLlmConfig(UserLlmConfig userLlmConfig);

    String getProvider(Long id);

    String selectApiKey(Long id);
}
