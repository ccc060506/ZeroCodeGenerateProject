package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamChunkMessage {
    private String type;   // "chunk" | "done" | "error"
    private String data;
    private String taskCode;
}
