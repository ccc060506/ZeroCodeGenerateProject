package com.ccc.zerocodegenerateproject.projectManager.controller;

import com.ccc.zerocodegenerateproject.common.result.Result;
import com.ccc.zerocodegenerateproject.common.util.DFAFilter;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectRankDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.SelectDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.SelectScrollDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectDetailVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectRankVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectVO;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/project")
public class projectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private DFAFilter dfaFilter;

    // 新建项目
    @PostMapping("/newCreate")
    public Result<String> newCreate(@RequestBody ProjectDTO projectDTO){
        log.info("收到新建项目请求，prompt长度: {}",
                projectDTO.getPrompt() != null ? projectDTO.getPrompt().length() : 0);
        String taskCode = projectService.newCreate(projectDTO);
        log.info("新建项目任务已提交，taskCode: {}", taskCode);
        return Result.success(taskCode, "项目生成任务已提交，正在生成中...");
    }

    // 查询历史项目---无条件
    @GetMapping("/scroll")
    public Result<List<ProjectVO>> list(SelectScrollDTO scrollDTO){
        log.info("查询历史项目---无条件");
        List<ProjectVO> list = projectService.selectAll(scrollDTO);
        if(list == null){
            return Result.error("当前无项目，请创建");
        }
        return Result.success(list);
    }

    // 查询历史项目---带条件
    @PostMapping("/scrollByRequest")
    public Result<List<ProjectVO>> listByRequest(@RequestBody SelectDTO selectDTO){
        log.info("查询项目---name: '{}', type: '{}', stack: '{}', isPublic: {}",
            selectDTO.getProjectName(), selectDTO.getProjectType(),
            selectDTO.getTechStack(), selectDTO.getIsPublic());
        List<ProjectVO> list = projectService.selectByRequest(selectDTO);
        if(list == null){
            return Result.error("无符合条件的项目");
        }
        return Result.success(list);
    }

    // 查看历史项目详情
    @GetMapping("/detail/{projectId}")
    public Result<ProjectDetailVO> detailById(@PathVariable Long projectId){
        log.info("查看项目 id 为 {} 的详情",projectId);
        ProjectDetailVO projectDetailVO = projectService.selectById(projectId);
        if(projectDetailVO == null){
            return Result.error("当前项目不存在或已被删除");
        }
        Boolean userLiked = projectService.isUserLiked(projectId);
        projectDetailVO.setLiked(userLiked);
        return Result.success(projectDetailVO);
    }

    // 删除项目
    @DeleteMapping("/delete/{projectId}")
    public Result<String> deleteById(@PathVariable Long projectId){
        log.info("删除 id 为 {} 的项目",projectId);
        Boolean b = projectService.checkProjectAccess(projectId);
        if(!b){
            return Result.error("您无访问权限");
        }
        Boolean res = projectService.deleteById(projectId);
        if(res){
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    // 项目重命名
    @PutMapping("/resetName/{projectId}")
    public Result<String> resetName(@PathVariable Long projectId,@RequestParam(value = "projectName") String newName){
        log.info("项目重命名为{}",newName);
        List<String> check = dfaFilter.check(newName);
        if(check != null && !check.isEmpty()){
            return Result.error("名称不合法,请重试");
        }
        projectService.resetName(projectId,newName);
        return Result.success("已重命名为" + newName);
    }

    // 主页查看所有项目
    @GetMapping("/home/project")
    public Result<List<ProjectVO>> allProject(){
        log.info("查询主页所有项目");
        List<ProjectVO> allProject = projectService.selectHomeAll();
        return Result.success(allProject);
    }

    // 点赞 或 取消点赞
    @PostMapping("/like/{projectId}")
    public Result<Long> likeOrUnlike(@PathVariable Long projectId,
                                     @RequestParam(value = "like") Boolean like){
        log.info("用户给项目id {} 进行点赞 {} ",projectId,like);
        Long likeCount = projectService.likeOrUnlike(projectId, like);
        return Result.success(likeCount);
    }

    // 排行榜---浏览量/点赞
    @PostMapping("/ranking")
    public Result<List<ProjectRankVO>> getProjectRank(@RequestBody ProjectRankDTO projectRankDTO){
        // 根据 浏览量 获取排行榜
        if(projectRankDTO.getIsLikeCount() == true && projectRankDTO.getIsViewCount() == false){
            log.info("用户 {} 根据浏览量获取排行榜" , UserContext.getId());
        }
        // 根据 点赞量 获取排行榜
        else {
            log.info("用户 {} 根据点赞量获取排行榜" , UserContext.getId());
        }
        List<ProjectRankVO> res = projectService.getProjectRank(projectRankDTO);
        return Result.success(res);
    }

    @GetMapping("/task/{taskCode}")
    public Result<ProjectGenerateTask> getTaskStatus(@PathVariable String taskCode) {
        ProjectGenerateTask task = projectService.getByTaskCode(taskCode);

        if (task == null) {
            return Result.success(null);
        }
        ProjectGenerateTask vo = new ProjectGenerateTask();
        BeanUtils.copyProperties(task, vo);
        return Result.success(vo);
    }
}
