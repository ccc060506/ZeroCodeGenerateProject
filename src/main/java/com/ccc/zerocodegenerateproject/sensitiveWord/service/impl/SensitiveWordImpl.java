package com.ccc.zerocodegenerateproject.sensitiveWord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.common.util.DFAFilter;
import com.ccc.zerocodegenerateproject.sensitiveWord.domain.entity.SensitiveWord;
import com.ccc.zerocodegenerateproject.sensitiveWord.mapper.SensitiveWordMapper;
import com.ccc.zerocodegenerateproject.sensitiveWord.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensitiveWordImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements ISensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    // 获取所有敏感词
    public List<String> getAllValidWords(){
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SensitiveWord::getWord);
        List<Object> objList = sensitiveWordMapper.selectObjs(wrapper);

        if (CollectionUtils.isEmpty(objList)) {
            return Collections.emptyList();
        }
        return objList.stream()
                .map(obj -> (String) obj)
                .collect(Collectors.toList());
    }

    // 构建和保存到redis
    public void buildAndSaveToRedis(List<String> words){
        DFAFilter dfaFilter = new DFAFilter();
        dfaFilter.buildDFA(words);
    }
}
