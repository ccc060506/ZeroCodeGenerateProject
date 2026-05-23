package com.ccc.zerocodegenerateproject.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ProjectCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger dailySequence = new AtomicInteger(1);

    @Autowired
    private ProjectMapper projectMapper;   // 注入 Mapper 用于查重

    /**
     * 生成全局唯一项目编码
     */
    public String generate() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        Long userId = UserContext.getId();
        String userSuffix = userId == null ? "000" : String.format("%03d", userId % 1000);

        int retry = 0;
        String projectCode;

        do {
            int seq = dailySequence.getAndIncrement();
            if (seq > 9999) {
                dailySequence.set(1);
                seq = 1;
            }

            projectCode = String.format("PROJ%s%04d%s", dateStr, seq, userSuffix);

            // 查数据库是否已存在
            long count = projectMapper.selectCount(
                    new LambdaQueryWrapper<Project>()
                            .eq(Project::getProjectCode, projectCode)
            );

            if (count == 0) {
                return projectCode;
            }

            retry++;
        } while (retry < 15);   // 最多重试15次

        // 极端情况：使用更随机的方式兜底
        long random = System.currentTimeMillis() % 10000;
        return String.format("PROJ%s%04d%s", dateStr, random, userSuffix);
    }

    /**
     * 每天0点重置序列
     */
    public void resetDailySequence() {
        dailySequence.set(1);
    }
}
