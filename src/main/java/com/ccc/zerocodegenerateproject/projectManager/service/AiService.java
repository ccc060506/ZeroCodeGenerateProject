package com.ccc.zerocodegenerateproject.projectManager.service;

import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;

public interface AiService {

    AiProjectSuggestion parseProjectFromPrompt(String prompt, UserLlmConfig config);
}
