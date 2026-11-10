package com.ryo.ryopicturebackend.Controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.StringUtils;
import com.ryo.ryopicturebackend.Common.BaseResponse;
import com.ryo.ryopicturebackend.Common.ResultUtils;
import com.ryo.ryopicturebackend.annotation.AuthCheck;
import com.ryo.ryopicturebackend.bizmq.BIProducer;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.exception.ThrowUtils;
import com.ryo.ryopicturebackend.model.dto.GenChartByAiRequest;
import com.ryo.ryopicturebackend.model.entity.Chart;
import com.ryo.ryopicturebackend.model.vo.BiResponse;
import com.ryo.ryopicturebackend.model.vo.ChartVO;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.ryo.ryopicturebackend.service.ChartService;
import com.ryo.ryopicturebackend.service.UserService;
import com.ryo.ryopicturebackend.utils.ExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 异步生成图表接口（基于 RabbitMQ 消息队列）
 * <p>
 * 与 {@code /chart/gen} 的区别只在触发方式，参数与返回结构保持一致：
 * <ul>
 *     <li>接口把任务数据（分析目标、图表类型、CSV 原始数据）保存到数据库，
 *     再把任务 id 投递到 RabbitMQ，接口立即返回，不占用请求线程等待 AI 结果；</li>
 *     <li>AI 调用由 MQ 消费者异步完成：消费者从数据库取出任务数据、调用 AI、
 *     把生成结果写回数据库，接口与 AI 调用之间彻底解耦。</li>
 * </ul>
 * 前端提交任务后拿到 chartId，用状态查询接口轮询任务进度即可。
 */
@Api(tags = "异步生成图表（MQ）")
@RestController//自动把每个接口的返回值转换成json格式 restful的api
@RequestMapping("/async/mq")
public class AsyncMqController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private BIProducer biProducer;

    /**
     * 提交 AI 生成图表的异步任务
     * <p>
     * 接口只负责保存任务数据并投递消息，AI 分析由消费者异步完成。
     *
     * @param multipartFile       上传的 Excel 文件
     * @param genChartByAiRequest AI 生成参数（名称、分析目标、图表类型）
     * @param request             当前请求，用于获取登录态
     * @return 新任务的 id，前端凭此 id 轮询生成结果
     */
    @ApiOperation("提交 AI 生成图表任务")
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度超过100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        //通过response对象拿到用户id 必须登录才能用
        UserLoginVO loginUserVO = userService.getLoginUserVO(request);

        //压缩后的数据，Excel 转 CSV
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        //存数据库：分析目标、图表类型、原始数据都入库，消费者凭任务 id 从这里取数据
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        //插入数据库时，还没生成结束，把生成结果都去掉，状态标记为排队中
        chart.setStatus("wait");
        chart.setUserId(loginUserVO.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR,"图表保存失败");

        //投递消息到 MQ，消息里只带任务 id，数据统一从数据库取
        String message = JSONUtil.createObj()
                .set("type", "genChart")
                .set("chartId", chart.getId())
                .toString();
        try {
            biProducer.sendMessage(message);
        } catch (Exception e) {
            //消息投递失败，消费者收不到，把任务标记为失败，避免任务一直卡在排队中
            Chart failedChart = new Chart();
            failedChart.setId(chart.getId());
            failedChart.setStatus("failed");
            failedChart.setExecMessage("消息投递失败：" + e.getMessage());
            chartService.updateById(failedChart);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务提交失败，请稍后重试");
        }

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 查询异步任务的状态与生成结果
     * <p>
     * status 取值：wait（排队中）、running（执行中）、succeed（已完成）、failed（失败）；
     * 任务完成后 genChart、genResult 字段为 AI 的生成结果。
     *
     * @param id      任务 id
     * @param request 当前请求，用于获取登录态
     * @return 任务详情
     */
    @ApiOperation("查询异步任务状态")
    @AuthCheck//必须登录才能查询自己的任务
    @GetMapping("/status/{id}")
    public BaseResponse<ChartVO> getTaskStatus(@PathVariable("id") Long id, HttpServletRequest request) {
        //复用图表详情查询，内部会校验任务归属，防止查看别人的任务
        return ResultUtils.success(chartService.getMyChartById(id, request));
    }
}
