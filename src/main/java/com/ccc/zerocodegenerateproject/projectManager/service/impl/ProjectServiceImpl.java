package com.ccc.zerocodegenerateproject.projectManager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.common.mq.RabbitMQConfig;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.*;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectDetailVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectRankVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectVO;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectGenerateTaskService;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectService;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ProjectGenerateTaskService taskService;
    private final RedisTemplate redisTemplate;
    private static final String RANK_VIEW_KEY = "rank:view_count";
    private static final String RANK_LIKE_KEY = "rank:like_count";
    private static final String LIKE_USERS_PREFIX = "project:like:users:";

    // 新建项目
    public String newCreate(ProjectDTO projectDTO) {
        log.info("收到新建项目请求，prompt: {}", projectDTO.getPrompt());

        // 1. 创建 PENDING 任务记录
        String taskCode = taskService.createPendingTask(projectDTO.getPrompt(), UserContext.getId());
        // 2. 构造消息并发送到 RabbitMQ 来触发异步进程
        ProjectGenerateMessage message = ProjectGenerateMessage.builder()
                .taskCode(taskCode)
                .userId(UserContext.getId())
                .genPrompt(projectDTO.getPrompt())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROJECT_GENERATE_EXCHANGE,
                RabbitMQConfig.PROJECT_GENERATE_ROUTING_KEY,
                message);
        log.info("生成任务已提交到 RabbitMQ，taskCode: {}", taskCode);
        return taskCode;
    }

    // 查询本人所有项目---无条件
    public List<ProjectVO> selectAll(SelectScrollDTO scrollDTO){
        // 参数校验和默认值处理
        if (scrollDTO.getSize() == null || scrollDTO.getSize() <= 0 || scrollDTO.getSize() > 100) {
            scrollDTO.setSize(20);
        }
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        // 只查询当前用户的数据
        wrapper.eq(Project::getUserId, UserContext.getId());
        // 滚动分页条件
        if (scrollDTO.getLastId() != null && scrollDTO.getLastCreateTime() != null) {
            wrapper.apply(
                    "(create_time < {0} OR (create_time = {0} AND id < {1}))",
                    scrollDTO.getLastCreateTime(),
                    scrollDTO.getLastId()
            );
        }
        // 倒序
        wrapper.orderByDesc(Project::getCreateTime)
                .orderByDesc(Project::getId)   // 防止同一毫秒创建的多条记录顺序混乱
                .last("LIMIT " + scrollDTO.getSize());

        List<Project> projects = projectMapper.selectList(wrapper);
        // 转换为 VO
        return projects.stream()
                .map(project -> {
                    ProjectVO projectVO = new ProjectVO();
                    BeanUtils.copyProperties(project, projectVO);
                    return projectVO;
                })
                .collect(Collectors.toList());
    }

    // 查询本人所有项目---带条件
    public List<ProjectVO> selectByRequest(SelectDTO selectDTO){
        if (selectDTO.getSize() == null || selectDTO.getSize() <= 0 || selectDTO.getSize() > 100) {
            selectDTO.setSize(20);
        }
        // 构建查询条件
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, UserContext.getId());

        // 动态条件
        if (StringUtils.hasText(selectDTO.getProjectName())) {
            String keyword = selectDTO.getProjectName().trim();
            log.info("按名称查询: '{}', userId: {}", keyword, UserContext.getId());
            wrapper.like(Project::getProjectName, keyword);
        } else {
            log.info("无条件查询所有项目, userId: {}", UserContext.getId());
        }
        if (selectDTO.getProjectType() != null) {
            wrapper.eq(Project::getProjectType, selectDTO.getProjectType());
        }
        if (StringUtils.hasText(selectDTO.getTechStack())) {
            wrapper.eq(Project::getTechStack, selectDTO.getTechStack());
        }
        if (selectDTO.getIsPublic() != null) {
            wrapper.eq(Project::getIsPublic, selectDTO.getIsPublic());
        }
        if (StringUtils.hasText(selectDTO.getProjectCode())) {
            wrapper.eq(Project::getProjectCode, selectDTO.getProjectCode());
        }

        // 滚动分页条件
        if (selectDTO.getLastId() != null && selectDTO.getLastCreateTime() != null) {
            wrapper.apply(" (create_time < {0} OR (create_time = {0} AND id < {1})) ",
                    selectDTO.getLastCreateTime(), selectDTO.getLastId());
        }

        // 根据时间倒序
        wrapper.orderByDesc(Project::getCreateTime)
                .orderByDesc(Project::getId)   // 防止同一时间创建的记录顺序不稳定
                .last("LIMIT " + selectDTO.getSize());

        List<Project> projects = projectMapper.selectList(wrapper);
        return projects.stream()
                .map(project -> {
                    ProjectVO projectVO = new ProjectVO();
                    BeanUtils.copyProperties(project,projectVO);
                    return projectVO;
                }).collect(Collectors.toList());
    }

    // 根据 id 查询详情
    public ProjectDetailVO selectById(Long id){
        Project project = projectMapper.selectById(id);
        // 记录浏览
        recordView(project.getId());
        ProjectDetailVO projectDetailVO = new ProjectDetailVO();
        BeanUtils.copyProperties(project,projectDetailVO);

        // 更新浏览量和点赞量
        projectDetailVO.setViewCount(getViewCountFromRedis(id));
        projectDetailVO.setLikeCount(getLikeCountFromRedis(id));
        projectDetailVO.setUsername(UserContext.getUsername());
        return projectDetailVO;
    }

    // 根据 id 删除项目
    public Boolean deleteById(Long id){
        int res  = projectMapper.deleteById(id);
        return res == 1;
    }

    // 项目重命名
    public void resetName(Long id, String newName){
        projectMapper.updateNameById(id,newName);
    }

    // 检查此项目是否属于此用户
    public Boolean checkProjectAccess(long projectId){
        Project project = projectMapper.selectById(projectId);
        return Objects.equals(project.getUserId(), UserContext.getId());
    }

    // 主页查看所有项目
    public List<ProjectVO> selectHomeAll(){
        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getIsPublic, 1)
                .orderByDesc(Project::getUpdateTime));
        return projects.stream().map(project -> {
            ProjectVO projectVO = new ProjectVO();
            BeanUtils.copyProperties(project,projectVO);
            return projectVO;
        }).collect(Collectors.toList());
    }

    // 记录一次浏览
    @Transactional(readOnly = true)
    public void recordView(Long projectId){
        if (projectId == null) return;

        String key = RANK_VIEW_KEY + ":" + projectId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForZSet().incrementScore(RANK_VIEW_KEY, projectId.toString(), 1);
        // 过期时间
        redisTemplate.expire(key,30, TimeUnit.DAYS);
    }

    // 点赞 或 取消点赞
    /**
     * @param projectId
     * @param like  true:like   false:unlike
     * @return
     */
    public Long likeOrUnlike(Long projectId,Boolean like){
        Long userId = UserContext.getId();
        String usersKey = LIKE_USERS_PREFIX + userId;

        // 点赞
        if(like){
            Long add = redisTemplate.opsForSet().add(usersKey, String.valueOf(userId));
            if(add != null && add > 0) {
                redisTemplate.opsForZSet().incrementScore(RANK_LIKE_KEY, projectId.toString(), 1);
            }
        }
        // 取消点赞
        else {
            Long remove = redisTemplate.opsForSet().remove(usersKey, String.valueOf(userId));
            if(remove != null && remove > 0){
                redisTemplate.opsForZSet().incrementScore(RANK_LIKE_KEY, projectId.toString(), -1);
            }
        }
        return getLikeCountFromRedis(projectId);
    }

    // 获取浏览量统计
    public Long getViewCountFromRedis(Long projectId) {
        if (projectId == null) return 0L;

        String key = RANK_VIEW_KEY + ":" + projectId;   // 建议加上冒号更清晰
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0L;
        }

        try {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Integer) {
                return ((Integer) value).longValue();     // ← 解决 Integer 转 Long
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
            log.warn("Redis浏览量转换失败 key={}, value={}", key, value, e);
        }
        return 0L;
    }

    // 获取点赞统计
    public Long getLikeCountFromRedis(Long projectId) {
        if (projectId == null) return 0L;

        String key = RANK_LIKE_KEY + ":" + projectId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0L;
        }

        try {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Integer) {
                return ((Integer) value).longValue();     // ← 解决 Integer 转 Long
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
            log.warn("Redis点赞量转换失败 key={}, value={}", key, value, e);
        }
        return 0L;
    }

    /**
     * 查询用户是否已点赞
     */
    public Boolean isUserLiked(Long projectId) {
        Long userId = UserContext.getId();
        if (projectId == null || userId == null) {
            return false;
        }
        String usersKey = LIKE_USERS_PREFIX + projectId;
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(usersKey, String.valueOf(userId))
        );
    }

    /**
     * 定时同步 Redis → MySQL
     */
    @Scheduled(cron = "0 0/10 * * * ?")   // 每10分钟同步一次
    @Transactional(rollbackFor = Exception.class)
    public void syncStatsToDatabase() {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(RANK_VIEW_KEY, 0, -1);


        if (tuples == null || tuples.isEmpty()) return;

        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            try {
                Long projectId = Long.valueOf(tuple.getValue().toString());
                long viewCount = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
                long likeCount = getLikeCountFromRedis(projectId);

                if (viewCount > 0 || likeCount > 0) {
                    projectMapper.updateStats(projectId, viewCount, likeCount);
                    log.info("同步统计数据 -> projectId: {}, viewCount: {}, likeCount: {}",
                            projectId, viewCount, likeCount);
                }
            } catch (Exception e) {
                log.error("同步单条统计数据失败，tuple: {}", tuple.getValue(), e);
            }
        }
    }

    // 获取排行榜
    public List<ProjectRankVO> getProjectRank(ProjectRankDTO projectRankDTO) {
        String zsetKey = projectRankDTO.getIsViewCount() ? RANK_VIEW_KEY : RANK_LIKE_KEY;

        // 拿到 <member, score> 集合（score 降序）
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, 19);

        if (tuples == null || tuples.isEmpty()) return new ArrayList<>();

        // 解析出有序的 projectId 列表 和 score Map
        List<Long> projectIds = new ArrayList<>();
        Map<Long, Long> scoreMap = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long pid = Long.valueOf(tuple.getValue().toString());
            long score = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
            projectIds.add(pid);
            scoreMap.put(pid, score);
        }

        // 批量查数据库
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        // 按排行榜顺序排列（selectBatchIds 不保证顺序）
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        return projectIds.stream()
                .map(pid -> {
                    Project p = projectMap.get(pid);
                    if (p == null) return null;
                    User user = userMapper.selectById(p.getUserId());
                    ProjectRankVO vo = new ProjectRankVO();
                    vo.setId(p.getId());
                    vo.setAvatar(user.getAvatar());
                    vo.setProjectName(p.getProjectName());
                    vo.setUsername(user.getUsername());
                    if (projectRankDTO.getIsViewCount()) {
                        vo.setViewCount(scoreMap.get(pid));
                        vo.setLikeCount(getLikeCountFromRedis(pid));
                    } else {
                        vo.setViewCount(getViewCountFromRedis(pid));
                        vo.setLikeCount(scoreMap.get(pid));
                    }
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 用于向前端展示生成状态
    public ProjectGenerateTask getByTaskCode(String taskCode){
        return projectMapper.getByTaskCode(taskCode);
    }
}
