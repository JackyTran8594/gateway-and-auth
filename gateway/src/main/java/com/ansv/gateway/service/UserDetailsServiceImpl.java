package com.ansv.gateway.service;

import com.ansv.gateway.constants.TypeRequestEnum;
import com.ansv.gateway.dto.mapper.UserMapper;
import com.ansv.gateway.dto.response.UserDTO;
import com.ansv.gateway.model.UserEntity;
import com.ansv.gateway.repository.UserEntityRepository;
import com.ansv.gateway.service.rabbitmq.RabbitMqSender;
import com.ansv.gateway.util.DataUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserDetailsServiceImpl implements CustomUserDetailService {

    @Value("${app.admin.username:#{null}}")
    private String usernameAdmin;

    @Value("${app.admin.password:#{null}}")
    private String passwordAdmin;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private RabbitMqSender rabbitMqSender;

    private ObjectMapper objectMapper;

    public UserDetailsServiceImpl() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username);

        User newUser = null;
        if (user != null) {
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new UsernameNotFoundException("User not found with username: ");
            }

            newUser = new User(user.getUsername(), user.getEmail(), buildSimpleGrantedAuthorities("user"));
        } else {
            // creating if user isn't exist in db
            log.warn("User not found with username ----> create in db", username);
            user = new UserEntity();
            user.setUsername(username);
            if (DataUtils.isNullOrEmpty(user.getEmail())) {
                user.setEmail(username);
            }
            user.setStatus("ACTIVE");
            userRepository.save(user);
            newUser = new User(user.getUsername(), user.getEmail(), buildSimpleGrantedAuthorities("user"));

            return newUser;
        }
        return newUser;
    }

    private static List<SimpleGrantedAuthority> buildSimpleGrantedAuthorities(final List<String> roles,
            List<String> roleList) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // for (Role role : roles) {
        // authorities.add(new SimpleGrantedAuthority(role.getName()));
        // }
        if (DataUtils.notNullOrEmpty(roleList)) {
            for (String role : roleList) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
        return authorities;
    }

    private static List<SimpleGrantedAuthority> buildSimpleGrantedAuthorities(String role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (DataUtils.isNullOrEmpty(role)) {
            role = "user";
        }
        authorities.add(new SimpleGrantedAuthority(role));
        return authorities;

    }

    @Override
    public UserDetails loadUser(String username, String displayName, String email) {
        UserEntity user = userRepository.findByUsername(username);
        User newUser = null;
        if (user != null) {
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new UsernameNotFoundException("User not found with username: ");
            }

            newUser = new User(user.getUsername(), user.getEmail(), buildSimpleGrantedAuthorities("user"));
        } else {
            // creating if user isn't exist in db
            log.warn("User not found with username ----> create in db", username);
            user = new UserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setFullname(displayName);
            user.setStatus("ACTIVE");
            userRepository.save(user);
            UserDTO userDTO = new UserDTO();
            userDTO = UserMapper.INSTANCE.modelToDTO(user);
            // rabbitMqSender.sender(userDTO);
            newUser = new User(username, email, buildSimpleGrantedAuthorities("user"));
            return newUser;
        }
        return newUser;
    }

    @Override
    public UserDTO findByUsername(String username) {
        UserEntity entity = userRepository.findByUsername(username);
        UserDTO dto = UserMapper.INSTANCE.modelToDTO(entity);
        return dto;
    }

    @Override
    public UserDetails loadUserDetails(String username, String displayName, String email) {
        UserDTO item = new UserDTO().builder().username(username).fullName(displayName).email(email).build();
        UserDTO userInfo = new UserDTO();
        // get user existed
        userInfo = getUserFromHumanService(item, TypeRequestEnum.VIEW.getName());
        if (!DataUtils.isNullOrEmpty(userInfo)) {
            User user = new User(userInfo.getUsername(), userInfo.getEmail(), buildSimpleGrantedAuthorities("user"));
            return user;
        } else {
            // add user 
            Object res = rabbitMqSender.senderUserObjectToHuman(item, TypeRequestEnum.INSERT.getName());
            if (!DataUtils.isNullOrEmpty(res)) {
                rabbitMqSender.sender(item);
                userInfo = (UserDTO) res;
                User user = new User(userInfo.getUsername(), userInfo.getEmail(), buildSimpleGrantedAuthorities("user"));
                return user;
            }
        }
        return null;

    }

    @Override
    public UserDetails loadUserByUsernameForInmemoryAuth(String username, String password) {
        if (DataUtils.notNullOrEmpty(username) && DataUtils.notNullOrEmpty(password)) {
            if (username.equals(usernameAdmin) && password.equals(passwordAdmin)) {
                UserDTO item = new UserDTO().builder().username(username).fullName(username).email(username).build();
                Object res = rabbitMqSender.senderUserObjectToHuman(item, TypeRequestEnum.INSERT.getName());
                if (!DataUtils.isNullOrEmpty(res)) {
                    rabbitMqSender.sender(item);
                    UserDTO userInfo = (UserDTO) res;
                    User user = new User(userInfo.getUsername(), userInfo.getEmail(), buildSimpleGrantedAuthorities("user"));
                    return user;
                }
            }
        }
        return null;
    }

    public UserDTO getUserFromHumanService(UserDTO user, String type) {
        // // TODO Auto-generated method stub
        try {
            Object obj = rabbitMqSender.senderUserToHumanService(user.getUsername(), type);
            log.info("------------" + obj.toString());
            if (!DataUtils.isNullOrEmpty(obj)) {
                UserDTO userDTO = objectMapper.readValue(obj.toString(), UserDTO.class);
                return userDTO;
            }
            return null;
        } catch (Exception e) {
            // TODO: handle exception
            log.error(e.getMessage());
            return null;
        }

    }
}
