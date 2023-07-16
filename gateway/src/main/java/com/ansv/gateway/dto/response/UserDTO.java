package com.ansv.gateway.dto.response;


import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO implements Serializable {
    private Long id;

    private String username;

    private String code;

    private String fullName;

    private String email;

    private String phone_number;

    private String position;

    private String note;

    private Long roleId;

    private Long departmentId;

    private String password;

    private String typeRequest;

    

}
