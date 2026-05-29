package com.company.imticket.infra.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange imExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_IM);
    }

    @Bean
    public Queue messageInQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_MESSAGE_IN)
                .deadLetterExchange(MqConstants.EXCHANGE_IM)
                .deadLetterRoutingKey("im.message.dlx")
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_NOTIFICATION).build();
    }

    @Bean
    public Queue messageDlxQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_MESSAGE_DLX).build();
    }

    @Bean
    public Binding messageInBinding() {
        return BindingBuilder.bind(messageInQueue())
                .to(imExchange()).with(MqConstants.ROUTING_KEY_MESSAGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(imExchange()).with(MqConstants.ROUTING_KEY_NOTIFICATION);
    }

    @Bean
    public Binding messageDlxBinding() {
        return BindingBuilder.bind(messageDlxQueue())
                .to(imExchange()).with("im.message.dlx");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}