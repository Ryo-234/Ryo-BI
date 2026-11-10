package com.ryo.ryopicturebackend.bizmq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.manager.AiManager;
import com.ryo.ryopicturebackend.model.entity.Chart;
import com.ryo.ryopicturebackend.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
/**
 * message 接收到的消息内容string
 * channel 消息所在的通道 可以通过该通道与RabbitMQ进行交互 例如手动确认拒绝消息
 * deliveryTag 消息投递标签 用于唯一标识一条消息
 */
//使用监听器注解 指定要监听的队列名称（BiMqConstant.BI_QUEUE_NAME）并设置消息的确认机制为手动确认
//@Header（amqpHeaders.delivery_tag）long deliverytag是一个方法参数注解 用于从消息头中获取传递的标签deliverytag
//注意：队列不存在时监听器会持续报错，程序启动前需先执行一次 BIInitMain 创建交换机、队列并完成绑定
//注意：@RabbitListener 必须标注在方法上，标注在类上不会被 Spring AMQP 识别，监听容器不会启动
public class BIConsumer {

    /**
     * 任务状态：执行中
     */
    private static final String STATUS_RUNNING = "running";

    /**
     * 任务状态：已完成
     */
    private static final String STATUS_SUCCEED = "succeed";

    /**
     * 任务状态：失败
     */
    private static final String STATUS_FAILED = "failed";

    /**
     * AI 回复中各段落的分隔符，回复格式为「图表配置」「分析结论」，共 5 段
     */
    private static final String SPLIT_FLAG = "【【【【【";

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    /**
     * 接收 MQ 消息并处理图表生成任务
     * <p>
     * 消息里只有任务 id，任务数据（分析目标、图表类型、CSV 原始数据）全部从数据库读取，
     * 这样接口与 AI 调用之间通过数据库和消息队列解耦。
     *
     * @param message    接收到的消息内容，格式为 {"type":"genChart","chartId":123}
     * @param channel    消息所在的通道，用于手动确认消息
     * @param deliveryTag 消息投递标签，用于唯一标识一条消息
     */
    //使用监听器注解 指定要监听的队列名称并设置消息的确认机制为手动确认
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG)long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        try {
            //从消息中取出任务 id
            Long chartId = this.parseChartId(message);
            if (chartId == null) {
                //消息格式无法识别，直接忽略，避免无效消息反复重投
                log.warn("图表生成任务消息格式不正确，message={}", message);
            } else {
                //从数据库取任务数据，调用 AI 生成图表并回写结果
                this.genChartByAi(chartId);
            }
            //手动确认消息的接收 向rabbitmq发送确认消息
            channel.basicAck(deliveryTag,false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行图表生成任务
     * <p>
     * 从数据库读取任务数据 → 调用 AI → 拆分 AI 回复 → 把结果写回数据库。
     *
     * @param chartId 任务 id（chart 表主键）
     */
    private void genChartByAi(Long chartId) {
        //从数据库读取任务数据：分析目标、图表类型、CSV 原始数据
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.warn("图表生成任务不存在，chartId={}", chartId);
            return;
        }
        //已经处理结束的任务不再重复执行（消息重投时的保护）
        if (STATUS_SUCCEED.equals(chart.getStatus()) || STATUS_FAILED.equals(chart.getStatus())) {
            log.info("图表生成任务已处理过，跳过，chartId={} status={}", chartId, chart.getStatus());
            return;
        }

        //更新状态为running
        Chart runningChart = new Chart();
        runningChart.setId(chartId);
        runningChart.setStatus(STATUS_RUNNING);
        chartService.updateById(runningChart);

        try {
            //构造 用户输入，格式与系统提示词中约定的格式一一对应
            StringBuilder userInput = new StringBuilder();
            userInput.append("分析需求：").append("\n");
            //拼接分析目标，指定图表类型
            String userGoal = chart.getGoal();
            if (StrUtil.isNotBlank(chart.getChartType())) {
                userGoal = userGoal + ",请使用" + chart.getChartType();
            }
            userInput.append(userGoal).append("\n");
            userInput.append("原始数据：").append("\n");
            userInput.append(chart.getChartData()).append("\n");

            //拿到返回结果
            String result = aiManager.doChat(userInput.toString());
            //对返回结果进行拆分
            String[] splits = result.split(SPLIT_FLAG);
            //对拆分结果校验
            if (splits.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI回复有误");
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            //调用AI得到结果再更新一次
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(STATUS_SUCCEED);
            chartService.updateById(updateChartResult);
            log.info("图表生成任务执行成功，chartId={}", chartId);
        } catch (Exception e) {
            //处理失败时把任务标记为failed并记录失败原因；
            //异常在这里消化掉不再往外抛，否则消息会被反复重投，任务反复失败
            log.error("图表生成任务执行失败，chartId={}", chartId, e);
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus(STATUS_FAILED);
            failedChart.setExecMessage(e.getMessage());
            chartService.updateById(failedChart);
        }
    }

    /**
     * 从消息中解析出任务 id
     * <p>
     * 正常情况下消息是 JSON 格式，同时兼容直接发送纯数字任务 id 的情况。
     *
     * @param message MQ 消息内容
     * @return 任务 id，解析不出来时返回 null
     */
    private Long parseChartId(String message) {
        if (StrUtil.isBlank(message)) {
            return null;
        }
        String trimmed = message.trim();
        //兼容只发了任务 id 的消息
        if (StrUtil.isNumeric(trimmed)) {
            return Long.parseLong(trimmed);
        }
        try {
            JSONObject jsonObject = JSONUtil.parseObj(trimmed);
            return jsonObject.getLong("chartId");
        } catch (Exception e) {
            log.error("解析图表生成任务消息失败，message={}", message, e);
            return null;
        }
    }
}
