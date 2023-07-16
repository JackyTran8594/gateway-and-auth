package com.ansv.authorizationserver.service.impl.rabbitmq;

import com.ansv.authorizationserver.dto.response.UserDTO;
import com.ansv.authorizationserver.service.impl.CustomUserDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class RabbitMqReceiver implements RabbitListenerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqReceiver.class);

    @Autowired
    private CustomUserDetailService customService;

    @Autowired
    private RabbitMqSender rabbitMqSender;

    @RabbitListener(queues = "${spring.rabbitmq.queue-received}")
    public void receivedMessage(String username) {
        logger.info("Username Received is.. " + username);
        UserDTO dto = customService.findByUsername(username);
        rabbitMqSender.sender(dto);
    }


    

//    @RabbitListener(queues = "${spring.rabbitmq.queue-received}")
//    public UserDTO receivedMessage(String username) {
//        logger.info("Username Received is.. " + username);
//        UserDTO dto = customService.findByUsername(username);
//        return dto;
//    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar rabbitListenerEndpointRegistrar) {

    }
}
