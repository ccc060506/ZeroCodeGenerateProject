package com.ccc.zerocodegenerateproject.common.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 项目生成相关常量
    public static final String PROJECT_GENERATE_EXCHANGE = "project.generate.exchange";
    public static final String PROJECT_GENERATE_QUEUE = "project.generate.queue";
    public static final String PROJECT_GENERATE_ROUTING_KEY = "project.generate";

    // 死信交换机和队列
    public static final String DLX_EXCHANGE = "project.dlx.exchange";
    public static final String DLX_QUEUE = "project.dlx.queue";
    public static final String DLX_ROUTING_KEY = "project.dlx";

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 设置 JSON 转换器
        template.setMessageConverter(jsonMessageConverter());

        // 开启 Mandatory，配合 ReturnsCallback（消息无法路由时触发）
        template.setMandatory(true);

        return template;
    }

    // 普通交换机（Direct）
    @Bean
    public DirectExchange projectGenerateExchange() {
        return ExchangeBuilder.directExchange(PROJECT_GENERATE_EXCHANGE).durable(true).build();
    }

    // 普通队列 + 绑定死信交换机
    @Bean
    public Queue projectGenerateQueue() {
        return QueueBuilder.durable(PROJECT_GENERATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)      // 绑定死信交换机
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY) // 死信 routing key
                .withArgument("x-message-ttl", 30 * 60 * 1000)             // 可选：队列消息存活30分钟，防止堆积
                .build();
    }

    @Bean
    public Binding projectGenerateBinding() {
        return BindingBuilder.bind(projectGenerateQueue())
                .to(projectGenerateExchange())
                .with(PROJECT_GENERATE_ROUTING_KEY);
    }

    // ====================== 死信相关 ======================
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }

    // 简单 RabbitMQ 监听器容器工厂,用于手动确认消息
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);  // 关键！支持手动ack
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
