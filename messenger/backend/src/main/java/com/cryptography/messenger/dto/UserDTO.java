package com.cryptography.messenger.dto;

import com.cryptography.messenger.enity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String name;

    public UserDTO(Users user) {
        id = user.getId().toString();
        name = user.getUsername();
    }
}
