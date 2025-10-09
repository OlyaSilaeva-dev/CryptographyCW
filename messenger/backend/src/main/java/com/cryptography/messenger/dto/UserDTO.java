package com.cryptography.messenger.dto;

import com.cryptography.messenger.enity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String name;

    public UserDTO(User user) {
        id = user.getId().toString();
        name = user.getUsername();
    }
}
