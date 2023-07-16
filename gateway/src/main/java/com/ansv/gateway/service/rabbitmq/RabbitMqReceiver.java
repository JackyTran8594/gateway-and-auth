package com.ansv.gateway.service.rabbitmq;

import com.ansv.gateway.dto.response.UserDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;

@Component
public class RabbitMqReceiver implements RabbitListenerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqReceiver.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserDTO userDTO = new UserDTO();

    public RabbitMqReceiver() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    // @RabbitListener(queues = { "${spring.rabbitmq.queue-received}" })
    // public void receivedMessage(String json) {
    //     logger.info("User Details Received is.. " + json);
    //     // userDTO = user;
    // }

    @RabbitListener(queues = "${spring.rabbitmq.queue-human-received}", concurrency = "3")
    public void receivedMessage(String jsonObject, Message message) {
        System.out.println(jsonObject + " -----------FROM HUMAN RESOURCE --------");
        System.out.println("CORRELATIONID: " + message.getMessageProperties().getCorrelationId());
        logger.info("JSON OBJECT FROM HUMAN: " + jsonObject);
    //    userDTO = objectMapper.readValue(jsonObject, UserDTO.class);
        // userDTO = user;
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar rabbitListenerEndpointRegistrar) {
        
    }

}
