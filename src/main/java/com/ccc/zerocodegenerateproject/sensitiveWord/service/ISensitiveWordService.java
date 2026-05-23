package com.ccc.zerocodegenerateproject.sensitiveWord.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.sensitiveWord.domain.entity.SensitiveWord;

import java.util.List;

public interface ISensitiveWordService extends IService<SensitiveWord> {

    List<String> getAllValidWords();

    void buildAndSaveToRedis(List<String> words);
}
