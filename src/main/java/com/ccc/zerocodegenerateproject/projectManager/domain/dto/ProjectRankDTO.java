package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRankDTO {

    private Boolean isViewCount;
    private Boolean isLikeCount;
}
