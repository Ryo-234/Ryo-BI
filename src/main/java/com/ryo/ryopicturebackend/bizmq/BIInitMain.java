package com.ryo.ryopicturebackend.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class BIInitMain {

    /**
     * 配置文件路径（本地环境配置优先级更高）
     */
    private static final String[] CONFIG_FILES = {"application.yml", "application-local.yml"};

    public static void main(String[] args) {
        try {
            // 创建连接工厂，连接信息从配置文件中读取，避免硬编码
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(getProperty("spring.rabbitmq.host", "localhost"));
            factory.setPort(Integer.parseInt(getProperty("spring.rabbitmq.port", "5672")));
            factory.setUsername(getProperty("spring.rabbitmq.username", "guest"));
            factory.setPassword(getProperty("spring.rabbitmq.password", "guest"));
            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
            // 定义交换机的名称
            String EXCHANGE_NAME = BiMqConstant.BI_EXCHANGE_NAME;
            // 声明交换机，指定交换机类型为 direct
            channel.exchangeDeclare(EXCHANGE_NAME, BiMqConstant.BI_EXCHANGE_TYPE);

            // 创建队列
            String queueName = BiMqConstant.BI_QUEUE_NAME;
            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 null
            channel.queueDeclare(queueName, true, false, false, null);
            // 将队列绑定到交换机，指定路由键
            channel.queueBind(queueName, EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);

            System.out.println("初始化完成：交换机[" + EXCHANGE_NAME + "] 队列[" + queueName
                    + "] 路由键[" + BiMqConstant.BI_ROUTING_KEY + "]");

            // 关闭连接与通道
            channel.close();
            connection.close();
        } catch (Exception e) {
            // 异常处理
            System.out.println("初始化交换机和队列失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从配置文件中读取配置项，取不到时返回默认值
     *
     * @param key          配置项名称
     * @param defaultValue 默认值
     * @return 配置项的值
     */
    private static String getProperty(String key, String defaultValue) {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = new ArrayList<>();
        for (String configFile : CONFIG_FILES) {
            try {
                propertySources.addAll(loader.load(configFile, new ClassPathResource(configFile)));
            } catch (IOException e) {
                System.out.println("读取配置文件[" + configFile + "]失败：" + e.getMessage());
            }
        }
        // 后加载的本地配置优先级更高，因此倒序查找
        for (int i = propertySources.size() - 1; i >= 0; i--) {
            Object value = propertySources.get(i).getProperty(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return defaultValue;
    }
}
