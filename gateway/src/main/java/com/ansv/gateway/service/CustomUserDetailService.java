package com.ansv.gateway.service;


import com.ansv.gateway.dto.response.UserDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface CustomUserDetailService extends UserDetailsService {

    UserDetails loadUser(String username, String displayName, String email) ;

    UserDetails loadUserByUsername(String username) ;

    UserDTO findByUsername(String username);

    // using with rabbitmq
    UserDetails loadUserDetails(String username, String displayName, String email);

    UserDetails loadUserByUsernameForInmemoryAuth(String username, String password);


}
