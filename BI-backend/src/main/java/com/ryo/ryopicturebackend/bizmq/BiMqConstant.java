package com.ryo.ryopicturebackend.bizmq;

/**
 * BI 消息队列常量
 * <p>
 * 注意：这里的交换机、队列、路由键统一使用 bi_ 前缀，
 * 与早期代码中硬编码的 code_exchange、code_queue、my_routingKey 并不相同，
 * 替换后需要在 RabbitMQ 中重新声明并绑定，否则消息无法路由到队列。
 */
public interface BiMqConstant {
    /**
     * 交换机名称
     */
    String BI_EXCHANGE_NAME = "bi_exchange";

    /**
     * 队列名称
     */
    String BI_QUEUE_NAME = "bi_queue";

    /**
     * 交换机路由键
     */
    String BI_ROUTING_KEY = "bi_routingKey";

    /**
     * 交换机类型
     */
    String BI_EXCHANGE_TYPE = "direct";
}
