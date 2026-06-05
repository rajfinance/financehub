package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileHeaderContext {
    private String displayUsername;
    private String displayFullName;
    private long profileAvatarVersion;
}
