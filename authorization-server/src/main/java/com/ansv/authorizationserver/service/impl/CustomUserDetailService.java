package com.ansv.authorizationserver.service.impl;


import com.ansv.authorizationserver.dto.response.UserDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface CustomUserDetailService extends UserDetailsService {

    UserDetails loadUser(String username, String displayName, String email) ;

    UserDTO findByUsername(String username);

}
