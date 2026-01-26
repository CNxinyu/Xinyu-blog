package com.xinyu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayload {
    private String receiver;
    private String scene;
    private String codeHash;
    private long sentAtEpochSec;
    private boolean verified; // 是否已校验通过，用于 ConsumeCode 流程
}
