package com.ryo.ryopicturebackend.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 管理器
 * 对接 DeepSeek 大模型，使用其兼容 OpenAI 的对话接口
 */
@Slf4j
@Service
public class AiManager {

    /**
     * 对话接口路径
     */
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /**
     * 最大输出 token 数上限
     */
    private static final int MAX_TOKENS_LIMIT = 8192;

    /**
     * 智能分析的系统提示词模板
     * 固定的分析要求与输出格式，调用 doChat 时自动生效
     */
    private static final String SYSTEM_PROMPT_TEMPLATE =
            "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n"
                    + "分析需求：\n"
                    + "{数据分析的需求或者目标}\n"
                    + "原始数据：\n"
                    + "{csv格式的原始数据，用,作为分隔符}\n"
                    + "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n"
                    + "【【【【【\n"
                    + "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n"
                    + "【【【【【\n"
                    + "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n"
                    + "【【【【【\n"
                    + "{\n"
                    + "    title: {\n"
                    + "        text: '网站用户增长情况',\n"
                    + "        subtext: ''\n"
                    + "        },\n"
                    + "    tooltip: {\n"
                    + "        trigger: 'axis',\n"
                    + "        axisPointer: {\n"
                    + "            type: 'shadow'\n"
                    + "            }\n"
                    + "    },\n"
                    + "    legend: {\n"
                    + "        data: ['用户数']\n"
                    + "        },\n"
                    + "    xAxis: {\n"
                    + "        data: ['1号', '2号', '3号']\n"
                    + "    },\n"
                    + "    yAxis: {},\n"
                    + "    series: [{\n"
                    + "        name: '用户数',\n"
                    + "        type: 'bar',\n"
                    + "        data: [10, 20, 30]\n"
                    + "    }]\n"
                    + "}\n"
                    + "【【【【【\n"
                    + "根据数据分析可得，该网站用户数量逐日增长，时间越长，用户数量增长越多。";

    /**
     * DeepSeek 接口地址
     */
    @Value("${deepseek.base-url}")
    private String baseUrl;

    /**
     * DeepSeek 的 API Key，本地环境放在 application-local.yml 中
     */
    @Value("${deepseek.api-key:}")
    private String apiKey;

    /**
     * 使用的模型名
     */
    @Value("${deepseek.model}")
    private String model;

    /**
     * 请求超时时间（毫秒）
     */
    @Value("${deepseek.timeout:120000}")
    private int timeout;

    /**
     * 最大输出 token 数，官方上限为 8192
     */
    @Value("${deepseek.max-tokens:4096}")
    private int maxTokens;

    /**
     * 构造系统提示词
     * 固定的数据分析与输出格式要求，调用 doChat 时自动带上
     *
     * @return 系统提示词
     */
    private String buildSystemPrompt() {
        return SYSTEM_PROMPT_TEMPLATE;
    }

    /**
     * 智能分析
     * 自动带上系统提示词，返回 ECharts 配置与分析结论
     *
     * @param goal    分析需求
     * @param csvData 原始数据，CSV 格式
     * @return 模型的分析结果
     */
    public String doChat(String goal, String csvData) {
        ThrowUtils.throwIf(StrUtil.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析需求为空");
        ThrowUtils.throwIf(StrUtil.isBlank(csvData), ErrorCode.PARAMS_ERROR, "原始数据为空");
        // 按系统提示词里的格式拼接，保证提示词与数据一一对应
        return doChat("分析需求：\n" + goal + "\n原始数据：\n" + csvData);
    }

    /**
     * 调用 DeepSeek 对话接口
     * 自动带上系统提示词，消息内容需包含分析需求与原始数据
     *
     * @param message 用户消息
     * @return 模型的分析结果
     */
    public String doChat(String message) {
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容为空");
        ThrowUtils.throwIf(StrUtil.isBlank(apiKey), ErrorCode.SYSTEM_ERROR, "DeepSeek 的 API Key 未配置");

        // 组装请求体，DeepSeek 兼容 OpenAI 的对话接口格式
        // reasoning_effort 设为 none 关闭思考模式，此时不会返回思维链内容，响应更快、费用更低
        String requestBody = JSONUtil.createObj()
                .set("model", model)
                .set("messages", JSONUtil.createArray()
                        .put(JSONUtil.createObj().set("role", "system")
                                .set("content", buildSystemPrompt()))
                        .put(JSONUtil.createObj().set("role", "user")
                                .set("content", message)))
                .set("thinking", JSONUtil.createObj().set("type", "disabled"))
                .set("reasoning_effort", "none")
                .set("max_tokens", Math.min(maxTokens, MAX_TOKENS_LIMIT))
                .set("stream", false)
                .toString();

        String url = baseUrl + CHAT_COMPLETIONS_PATH;
        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(timeout)
                .execute()) {
            if (!response.isOk()) {
                log.error("调用 DeepSeek 失败，状态码：{}，响应内容：{}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 分析失败，请稍后重试");
            }
            // 回答被 max_tokens 截断时内容不完整，直接提示用户，避免上层拿到残缺结果
            JSONObject result = JSONUtil.parseObj(response.body());
            if ("length".equals(result.getByPath("choices[0].finish_reason", String.class))) {
                log.warn("DeepSeek 回答超出 max_tokens 上限被截断，maxTokens={}", Math.min(maxTokens, MAX_TOKENS_LIMIT));
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 回答过长被截断，请缩小数据范围后重试");
            }
            // 取第一条消息的内容
            String content = result.getByPath("choices[0].message.content", String.class);
            if (StrUtil.isBlank(content)) {
                log.error("DeepSeek 未返回有效内容，响应内容：{}", response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 分析结果为空，请稍后重试");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 DeepSeek 异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 分析失败，请稍后重试");
        }
    }
}
