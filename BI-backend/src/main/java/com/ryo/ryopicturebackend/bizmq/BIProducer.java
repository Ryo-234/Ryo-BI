package com.ryo.ryopicturebackend.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BIProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到 BI 专用交换机
     * 交换机与路由键均取自 BiMqConstant，避免调用处硬编码字符串
     *
     * @param message 消息内容
     */
    public void sendMessage(String message) {
        //使用rabbittemplate的convertAndSend方法将消息发送到指定的交换机和路由键
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }

    /**
     * 使用自定义的交换机与路由键发送消息
     *
     * @param exchange 交换机名称 指定消息要发送到哪个交换机
     * @param routingKey 路由键，指定消息要根据什么规则路由到相应的队列
     * @param message 消息内容
     */
    public void sendMessage(String exchange, String routingKey, String message) {
        //使用rabbittemplate的convertAndSend方法将消息发送到指定的交换机和路由键
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
