package com.ccc.zerocodegenerateproject.codeGenerateModule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.dto.ModifyDSLDTO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageVersion;
import com.ccc.zerocodegenerateproject.codeGenerateModule.mapper.ModifyCodeMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.mapper.PageVersionMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ModifyCodeService;
import com.ccc.zerocodegenerateproject.codeGenerateModule.util.DSLModifier;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class ModifyCodeServiceImpl extends ServiceImpl<ModifyCodeMapper, Project> implements ModifyCodeService {

    @Autowired
    private ModifyCodeMapper modifyCodeMapper;
    @Autowired
    private DSLEngine dslEngine;
    @Autowired
    private DSLModifier dslModifier;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private PageVersionMapper pageVersionMapper;
    @Autowired
    private ProjectMapper projectMapper;

    // 修改组件
    @Transactional
    public Boolean updateComponent(Long projectId ,Long pageId,Integer versionNum, ModifyDSLDTO json){

        // 拿到当前处理的版本
        Integer baseVersion = versionNum;
        if (baseVersion == null) {
            // 去数据库查该页面的最大版本号
            PageVersion maxV = pageVersionMapper.selectOne(new LambdaQueryWrapper<PageVersion>()
                    .eq(PageVersion::getPageDSLId, pageId)
                    .orderByDesc(PageVersion::getVersionNum).last("LIMIT 1"));
            baseVersion = (maxV != null) ? maxV.getVersionNum() : 1;
        }

        String pageCacheKey = "project:page:id:" + pageId + ":v:" + baseVersion;
        String projectCacheKey = "project:id:" + projectId;

        // 查询 redis 中该项目
        Project project = (Project) redisTemplate.opsForValue().get(projectCacheKey);
        if (project == null) {
            // 从数据库查询此项目
            project = projectMapper.selectById(projectId);
            if(project == null){
                throw new RuntimeException("此项目不存在");
            }
            redisTemplate.opsForValue().set(projectCacheKey,project,7,TimeUnit.DAYS);

        }

        CodeDSL codeDSL = dslEngine.parse(project.getCodeDSLJson());

        // 查询 redis 中该页面
        String dslJson = (String) redisTemplate.opsForValue().get(pageCacheKey);
        PageVersion pageVersion;
        if (dslJson == null || !dslEngine.parsePage(dslJson).getCurrentVersion().equals(baseVersion)) {
            // 从 PageVersion 表查找该 pageDSLId 对应的一条记录
            pageVersion = pageVersionMapper.selectOne(new LambdaQueryWrapper<PageVersion>()
                    .eq(PageVersion::getPageDSLId, pageId)
                    .eq(PageVersion::getVersionNum, baseVersion)
            );
            if (pageVersion == null) {
                throw new RuntimeException("页面不存在");
            }
            redisTemplate.opsForValue().set(pageCacheKey,pageVersion.getDslJson(),7,TimeUnit.DAYS);
            dslJson = pageVersion.getDslJson();
        }

        // 解析得到对应的实体类

        PageDSL pageDSL = dslEngine.parsePage(dslJson);
        List<PageDSL> pages = codeDSL.getPages();
        boolean found = false;
        for (int i = 0; i < pages.size(); i++) {
            if (Objects.equals(pages.get(i).getId(), pageId)) {
                pages.set(i, pageDSL);
                found = true;
                break;
            }
        }
        if (!found) throw new RuntimeException("项目 DSL 中未找到匹配的页面 ID");
        // 执行 修改
        CodeDSL applyDSL = dslModifier.apply(codeDSL, json.getOperations());
        PageDSL updatedPage = dslEngine.findPage(applyDSL, pageId, null);
        // 拿到修改之后的 PageJson 和 ProjectJson
        String newPageJson = dslEngine.stringify(updatedPage);
        String newProjectJson = dslEngine.stringify(applyDSL);

        // 更新两个 Redis 缓存
        redisTemplate.opsForValue().set(pageCacheKey, newPageJson, 1, TimeUnit.DAYS);
        // Project 实体里的 CodeDSLJson 也需要同步更新
        project.setCodeDSLJson(newProjectJson);
        redisTemplate.opsForValue().set(projectCacheKey, project, 1, TimeUnit.DAYS);

        return true;
    }

    // 保存修改
    public Boolean saveProject(Long projectId){

        String cacheKey = "project:edit:" + projectId;
        String cachedDSL = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedDSL == null) {
            // 如果缓存为空，说明用户没改过或者缓存过期，直接返回
            return true;
        }

        // 获取当前数据库中该项目的最新一条记录，为了拿到基础信息和当前版本号
        // 查询最新版本
        Project latestVersion = modifyCodeMapper.selectOne(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, projectId)
                .orderByDesc(Project::getVersion)
                .last("LIMIT 1")
        );
        if (latestVersion == null) {
            throw new RuntimeException("初始项目不存在，无法保存历史版本");
        }

        Project newHistoryVersion = new Project();
        BeanUtils.copyProperties(latestVersion, newHistoryVersion);

        //  更新关键字段
        newHistoryVersion.setCodeDSLJson(cachedDSL);
        newHistoryVersion.setVersion(latestVersion.getVersion() + 1); // 版本号递增
        newHistoryVersion.setUpdateTime(LocalDateTime.now());
        newHistoryVersion.setCreateTime(LocalDateTime.now());

        int insertCount = modifyCodeMapper.insert(newHistoryVersion);

        if (insertCount > 0) {
            // 保存成功后，删除编辑缓存
            redisTemplate.delete(cacheKey);
            return true;
        }
        return false;
    }
}
