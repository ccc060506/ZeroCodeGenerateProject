package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.dto;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.Operation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifyDSLDTO {

    private Long sessionId;      // 会话ID（用于撤销/重做）
    private Long timestamp;      // 时间戳（解决并发冲突）
    private Long userId;         // 用户ID（权限校验）
    private Integer version;     // 乐观锁版本号（防止覆盖）
    private List<Operation> operations;
}
