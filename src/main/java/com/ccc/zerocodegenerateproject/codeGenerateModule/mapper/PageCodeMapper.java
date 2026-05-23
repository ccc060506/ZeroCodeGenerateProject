package com.ccc.zerocodegenerateproject.codeGenerateModule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageVersion;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PageCodeMapper extends BaseMapper<Project> {
    List<PageVersion> getVersionList(long pageId);

    Boolean deleteByVersion(long pageId, Integer version);
}
