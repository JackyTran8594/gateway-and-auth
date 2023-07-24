package com.ansv.gateway.service.rabbitmq;

import com.ansv.gateway.dto.response.UserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RabbitMqSender {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqSender.class);

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange:#{null}}")
    private String exchange;

    // to task
    @Value("${spring.rabbitmq.routingkey-task:#{null}}")
    private String routingkeyTask;

    // to human
    @Value("${spring.rabbitmq.routingkey-human:#{null}}")
    private String routingkeyHuman;

    // to human
    @Value("${spring.rabbitmq.queue-human-received:#{null}}")
    private String queueHumanReceived;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMqSender() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    // sender to task service
    public void sender(UserDTO user) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingkeyTask, objectMapper.writeValueAsString(user));
        } catch (JsonProcessingException | AmqpException e) {
            // TODO Auto-generated catch block
            logger.info(e.getMessage());
        }
    }
    // end

    // sender to human service for check username
    public void senderUsernameToHuman(UserDTO user) {
        UUID correlationId = UUID.randomUUID();
        MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {

            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // TODO Auto-generated method stub
                MessageProperties messageProperties = message.getMessageProperties();
                messageProperties.setReplyTo(queueHumanReceived);
                messageProperties.setCorrelationId(correlationId.toString());
                return message;
            }

        };

        Object obj = rabbitTemplate.convertSendAndReceive(exchange, routingkeyHuman, user, messagePostProcessor);

    }
    // end

    // sender userObject to task service for add user
    public void senderUserToTask(UserDTO item, String type) throws JsonProcessingException, AmqpException {
        rabbitTemplate.convertAndSend(exchange, routingkeyTask, objectMapper.writeValueAsString(item));
    }

    public Object senderTest(String username) {
        Object response = rabbitTemplate.convertSendAndReceive(exchange, routingkeyHuman, username);
        logger.info("------------RESPONSE FROM HUMAN:" + response.toString());
        return response;
    }

    // send username to Humane service
    public Object senderUserToHumanService(String username, String type) {
        Object response = rabbitTemplate.convertSendAndReceive(exchange, routingkeyHuman, username,
                new MessagePostProcessor() {

                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // TODO Auto-generated method stub
                        message.getMessageProperties().setHeader("typeRequest", type);
                        return message;
                    }

                });
        logger.info("---------- RESPONSE FROM HUMAN:" + response.toString());
        return response;
    }

    // send Object to Humane service
    public Object senderUserObjectToHuman(UserDTO item, String type) {
        try {
            Object response = rabbitTemplate.convertSendAndReceive(exchange, routingkeyHuman,
                    objectMapper.writeValueAsString(item), new MessagePostProcessor() {

                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            // TODO Auto-generated method stub
                            message.getMessageProperties().setHeader("typeRequest", type);
                            return message;
                        }

                    });
            logger.info("---------- RESPONSE FROM HUMAN:" + response.toString());
            return response;
        } catch (Exception e) {
            // TODO: handle exception
            logger.error(e.getMessage());
            return null;
        }

    }

}
