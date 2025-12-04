package com.rag.ops.document.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {
    
    public static final String DOCUMENT_PROCESSING_EXCHANGE = "document.processing.exchange";
    public static final String DOCUMENT_PROCESSING_QUEUE = "document.processing.queue";
    public static final String DOCUMENT_PROCESSING_ROUTING_KEY = "document.processing";
    
    public static final String BATCH_PROCESSING_QUEUE = "document.batch.processing.queue";
    public static final String BATCH_PROCESSING_ROUTING_KEY = "document.batch.process";
    
    /**
     * 文档处理交换机
     */
    @Bean
    public DirectExchange documentProcessingExchange() {
        return new DirectExchange(DOCUMENT_PROCESSING_EXCHANGE, true, false);
    }
    
    /**
     * 文档处理队列
     */
    @Bean
    public Queue documentProcessingQueue() {
        return QueueBuilder.durable(DOCUMENT_PROCESSING_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 1小时过期
                .build();
    }
    
    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding documentProcessingBinding() {
        return BindingBuilder
                .bind(documentProcessingQueue())
                .to(documentProcessingExchange())
                .with(DOCUMENT_PROCESSING_ROUTING_KEY);
    }
    
    /**
     * 批量处理队列
     */
    @Bean
    public Queue batchProcessingQueue() {
        return QueueBuilder.durable(BATCH_PROCESSING_QUEUE)
                .withArgument("x-message-ttl", 7200000) // 2小时过期
                .build();
    }
    
    /**
     * 绑定批量处理队列到交换机
     */
    @Bean
    public Binding batchProcessingBinding() {
        return BindingBuilder
                .bind(batchProcessingQueue())
                .to(documentProcessingExchange())
                .with(BATCH_PROCESSING_ROUTING_KEY);
    }
    
    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
