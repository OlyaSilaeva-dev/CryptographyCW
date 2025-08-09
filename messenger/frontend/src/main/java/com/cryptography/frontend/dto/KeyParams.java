package com.cryptography.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeyParams {
//    private String senderId;
//    private String recipientId;
    private String p;
    private String g;
}
