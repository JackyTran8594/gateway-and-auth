package com.ansv.gateway.service;

import com.ansv.gateway.constants.TypeRequestEnum;
import com.ansv.gateway.dto.mapper.UserMapper;
import com.ansv.gateway.dto.response.UserDTO;
import com.ansv.gateway.model.UserEntity;
import com.ansv.gateway.repository.UserEntityRepository;
import com.ansv.gateway.service.rabbitmq.RabbitMqReceiver;
import com.ansv.gateway.service.rabbitmq.RabbitMqSender;
import com.ansv.gateway.util.DataUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
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

    // @Autowired
    // private RabbitMqReceiver rabbitMqReceiver;

    @Autowired
    private AmqpTemplate rabbitMqTemplate;

    private RestTemplate restTemplate;

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
        item.setTypeRequest(TypeRequestEnum.VIEW.getName());
        userInfo = getUserFromHumanService(item);
        if (!DataUtils.isNullOrEmpty(userInfo)) {
            User user = new User(item.getUsername(), item.getEmail(), buildSimpleGrantedAuthorities("user"));
            return user;
        } else {
            item.setTypeRequest(TypeRequestEnum.INSERT.getName());
            rabbitMqSender.senderUserObject(item);
            rabbitMqSender.sender(item);
            // UserDTO userDTO = rabbitMqReceiver.userDTO;
            // clear user để nhận lần tiếp theo
            // rabbitMqReceiver.userDTO = new UserDTO();
            // if (DataUtils.notNull(userDTO)) {
            //     User user = new User(username, email, buildSimpleGrantedAuthorities("user"));
            //     return user;
            // }
        }
        return null;

    }

    @Override
    public UserDetails loadUserByUsernameForInmemoryAuth(String username, String password) {
        if (DataUtils.notNullOrEmpty(username) && DataUtils.notNullOrEmpty(password)) {
            if (username.equals(usernameAdmin) && password.equals(passwordAdmin)) {
                UserDTO item = new UserDTO().builder().username(username).fullName(username).email(username).build();
                rabbitMqSender.senderUserObject(item);
                // rabbitMqSender.sender(item);
                User user = new User(username, username, buildSimpleGrantedAuthorities("ADMIN"));
                return user;
            }
        }
        return null;
    }

    // find user
    @Override
    public UserDetails loadUserByUsernameFromHumanResource(UserDTO item) {
        // UserDTO item = new
        // UserDTO().builder().username(username).fullName(username).email(username).build();
        rabbitMqSender.senderUsernameToHuman(item);
        // UserDTO userDTO = rabbitMqReceiver.userDTO;

        // clear user
        // rabbitMqReceiver.userDTO = new UserDTO();
        // if (DataUtils.notNull(userDTO) && !DataUtils.isNullOrEmpty(userDTO.getUsername())) {
        //     User user = new User(item.getUsername(), userDTO.getEmail(), buildSimpleGrantedAuthorities("user"));
        //     return user;
        // }
        return null;
    }

    @Override
    public UserDTO getUserFromHumanService(UserDTO user) {
        // TODO Auto-generated method stub
        // rabbitMqSender.senderUserObject(user);
        rabbitMqSender.senderUsernameToHuman(user);
        // UserDTO userDTO = rabbitMqReceiver.userDTO;
        // // UserDTO userDTO = rabbitMqSender.sendAndReceiveUser(user);

        // if (DataUtils.notNull(userDTO) && !DataUtils.isNullOrEmpty(userDTO.getUsername())) {
        //     return userDTO;
        // }
        return null;
    }
}
