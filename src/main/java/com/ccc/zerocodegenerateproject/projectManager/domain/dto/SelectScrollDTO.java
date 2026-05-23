package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectScrollDTO {
    private Long lastId;        // 上次返回的最后一条记录的 id（推荐用主键）
    private LocalDateTime lastCreateTime; // 防止同一时间创建的多条记录漏掉
    private Integer size = 20;  // 每页大小，默认20条
}
