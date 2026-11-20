package com.ryo.ryopicturebackend.service;

import com.ryo.ryopicturebackend.model.dto.ChartCreateRequest;
import com.ryo.ryopicturebackend.model.dto.ChartQueryRequest;
import com.ryo.ryopicturebackend.model.dto.ChartUpdateRequest;
import com.ryo.ryopicturebackend.model.entity.Chart;
import com.ryo.ryopicturebackend.model.vo.ChartVO;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @author admin
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2026-11-08 19:52:08
*/
public interface ChartService extends IService<Chart> {

    /**
     * 创建图表，创建人取当前登录用户
     *
     * @param chartCreateRequest 创建请求参数
     * @param request            当前请求，用于获取登录态
     * @return 新图表 id
     */
    long createChart(ChartCreateRequest chartCreateRequest, HttpServletRequest request);

    /**
     * 根据 id 修改自己的图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @param request            当前请求，用于获取登录态
     * @return 是否修改成功
     */
    boolean updateMyChart(ChartUpdateRequest chartUpdateRequest, HttpServletRequest request);

    /**
     * 根据 id 删除自己的图表
     *
     * @param id      图表 id
     * @param request 当前请求，用于获取登录态
     * @return 是否删除成功
     */
    boolean deleteMyChart(Long id, HttpServletRequest request);

    /**
     * 根据 id 查看自己的图表详情
     *
     * @param id      图表 id
     * @param request 当前请求，用于获取登录态
     * @return 图表详情
     */
    ChartVO getMyChartById(Long id, HttpServletRequest request);

    /**
     * 分页查询自己的图表列表，每页最多 20 条，支持按图表名称查询
     *
     * @param chartQueryRequest 查询参数
     * @param request           当前请求，用于获取登录态
     * @return 分页后的图表列表
     */
    PageResultVO<ChartVO> listMyChartByPage(ChartQueryRequest chartQueryRequest, HttpServletRequest request);

    /**
     * 管理员根据 id 删除任意图表
     *
     * @param id 图表 id
     * @return 是否删除成功
     */
    boolean adminDeleteChart(Long id);

    /**
     * 管理员根据 id 更新任意图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    boolean adminUpdateChart(ChartUpdateRequest chartUpdateRequest);

    /**
     * 管理员分页查询图表列表，支持按除时间外的任意字段查询，每页数量不限
     *
     * @param chartQueryRequest 查询参数
     * @return 分页后的图表列表
     */
    PageResultVO<ChartVO> adminListChart(ChartQueryRequest chartQueryRequest);

    /**
     * 管理员根据 id 查看任意图表详情
     *
     * @param id 图表 id
     * @return 图表详情
     */
    ChartVO adminGetChartById(Long id);

    /**
     * 将查询请求转为 QueryWrapper 查询条件，用于构造 SQL
     *
     * @param chartQueryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);
}
