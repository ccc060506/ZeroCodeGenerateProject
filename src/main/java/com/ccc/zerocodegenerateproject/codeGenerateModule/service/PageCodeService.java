package com.ccc.zerocodegenerateproject.codeGenerateModule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.GeneratePageResult;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVersionVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PageCodeService extends IService<Project> {

    PageVO getPageInfo(long projectId , long pageId, Integer versionNum);

    List<PageVersionVO> getVersionList(long pageId);

    // 页面是否属于该项目
    Boolean checkPageBelongsToProject(long projectId , long pageId);

    Boolean deleteByVersion(long pageId, Integer version);

    Flux<ServerSentEvent<Object>> generateNewPageStream(Long projectId, Long pageId, String prompt, UserLlmConfig userLlmConfig);

    /** 从数据库直接查某页面的最新生成代码（用于前端刷新兜底） */
    String getLatestPageCode(long projectId, long pageId);

    /** 从数据库直接查项目的完整 codeDSLJson（用于前端刷新兜底） */
    String getProjectCodeDsl(long projectId);
}
