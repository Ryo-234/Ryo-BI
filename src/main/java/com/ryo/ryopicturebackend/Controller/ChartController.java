package com.ryo.ryopicturebackend.Controller;

import com.ryo.ryopicturebackend.Common.BaseResponse;
import com.ryo.ryopicturebackend.Common.ResultUtils;
import com.ryo.ryopicturebackend.annotation.AuthCheck;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.exception.ThrowUtils;
import com.ryo.ryopicturebackend.model.dto.ChartCreateRequest;
import com.ryo.ryopicturebackend.model.dto.ChartQueryRequest;
import com.ryo.ryopicturebackend.model.dto.ChartUpdateRequest;
import com.ryo.ryopicturebackend.model.vo.ChartVO;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.ryo.ryopicturebackend.service.ChartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 图表接口
 */
@Api(tags = "图表管理")
@RestController//自动把每个接口的返回值转换成json格式 restful的api
@RequestMapping("/chart")
public class ChartController {

    @Resource
    private ChartService chartService;

    /**
     * 【用户】创建图表，需上传分析目标、图表数据、图表类型
     *
     * @param chartCreateRequest 创建请求参数
     * @param request            当前请求
     * @return 新图表 id
     */
    @ApiOperation("创建图表")
    @AuthCheck//必须登录才能创建
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartCreateRequest chartCreateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chartCreateRequest==null, ErrorCode.PARAMS_ERROR);
        long result = chartService.createChart(chartCreateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 【用户】根据 id 修改自己的图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @param request            当前请求
     * @return 是否修改成功
     */
    @ApiOperation("修改自己的图表")
    @AuthCheck//必须登录才能修改
    @PutMapping("/update")
    public BaseResponse<Boolean> updateMyChart(@RequestBody ChartUpdateRequest chartUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chartUpdateRequest==null, ErrorCode.PARAMS_ERROR);
        boolean result = chartService.updateMyChart(chartUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 【用户】根据 id 删除自己的图表
     *
     * @param id      图表 id
     * @param request 当前请求
     * @return 是否删除成功
     */
    @ApiOperation("删除自己的图表")
    @AuthCheck//必须登录才能删除
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteMyChart(@PathVariable("id") Long id, HttpServletRequest request) {
        boolean result = chartService.deleteMyChart(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 【用户】根据 id 查看自己的图表详情
     *
     * @param id      图表 id
     * @param request 当前请求
     * @return 图表详情
     */
    @ApiOperation("查看自己的图表详情")
    @AuthCheck//必须登录才能查看
    @GetMapping("/get/{id}")
    public BaseResponse<ChartVO> getMyChartById(@PathVariable("id") Long id, HttpServletRequest request) {
        ChartVO chartVO = chartService.getMyChartById(id, request);
        return ResultUtils.success(chartVO);
    }

    /**
     * 【用户】分页查询自己的图表列表，支持按名称查询，每页最多 20 个
     *
     * @param chartQueryRequest 查询参数
     * @param request           当前请求
     * @return 分页后的图表列表
     */
    @ApiOperation("分页查询自己的图表列表")
    @AuthCheck//必须登录才能查询
    @GetMapping("/list")
    public BaseResponse<PageResultVO<ChartVO>> listMyChartByPage(ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        PageResultVO<ChartVO> pageResultVO = chartService.listMyChartByPage(chartQueryRequest, request);
        return ResultUtils.success(pageResultVO);
    }

    /**
     * 【管理员】根据 id 删除任意图表
     *
     * @param id 图表 id
     * @return 是否删除成功
     */
    @ApiOperation("管理员删除图表")
    @AuthCheck(mustRole = "admin")
    @DeleteMapping("/admin/delete/{id}")
    public BaseResponse<Boolean> adminDeleteChart(@PathVariable("id") Long id) {
        boolean result = chartService.adminDeleteChart(id);
        return ResultUtils.success(result);
    }

    /**
     * 【管理员】根据 id 更新任意图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    @ApiOperation("管理员更新图表")
    @AuthCheck(mustRole = "admin")
    @PutMapping("/admin/update")
    public BaseResponse<Boolean> adminUpdateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        ThrowUtils.throwIf(chartUpdateRequest==null, ErrorCode.PARAMS_ERROR);
        boolean result = chartService.adminUpdateChart(chartUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 【管理员】分页查询图表列表，支持按除时间外的任意字段查询，每页数量不限
     *
     * @param chartQueryRequest 查询参数
     * @return 分页后的图表列表
     */
    @ApiOperation("管理员分页查询图表列表")
    @AuthCheck(mustRole = "admin")
    @GetMapping("/admin/list")
    public BaseResponse<PageResultVO<ChartVO>> adminListChart(ChartQueryRequest chartQueryRequest) {
        PageResultVO<ChartVO> pageResultVO = chartService.adminListChart(chartQueryRequest);
        return ResultUtils.success(pageResultVO);
    }

    /**
     * 【管理员】根据 id 查看任意图表详情
     *
     * @param id 图表 id
     * @return 图表详情
     */
    @ApiOperation("管理员查看图表详情")
    @AuthCheck(mustRole = "admin")
    @GetMapping("/admin/get/{id}")
    public BaseResponse<ChartVO> adminGetChartById(@PathVariable("id") Long id) {
        ChartVO chartVO = chartService.adminGetChartById(id);
        return ResultUtils.success(chartVO);
    }
}
