package com.ryo.ryopicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.mapper.ChartMapper;
import com.ryo.ryopicturebackend.model.dto.ChartCreateRequest;
import com.ryo.ryopicturebackend.model.dto.ChartQueryRequest;
import com.ryo.ryopicturebackend.model.dto.ChartUpdateRequest;
import com.ryo.ryopicturebackend.model.entity.Chart;
import com.ryo.ryopicturebackend.model.vo.ChartVO;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.ryo.ryopicturebackend.service.ChartService;
import com.ryo.ryopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author admin
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2026-11-08 19:52:08
*/
@Slf4j
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    //用户端分页每页最多返回的条数
    private static final int USER_MAX_PAGE_SIZE = 20;
    //默认每页条数，页面大小非法时使用
    private static final int DEFAULT_PAGE_SIZE = 10;
    //允许排序的字段白名单，排序字段由前端传入，必须校验防止 SQL 注入
    private static final Set<String> SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id", "name", "goal", "chartType", "genChart", "genResult", "userId", "createTime", "updateTime"));

    @Resource
    private UserService userService;

    /**
     * 创建图表，创建人取当前登录用户
     *
     * @param chartCreateRequest 创建请求参数
     * @param request            当前请求，用于获取登录态
     * @return 新图表 id
     */
    @Override
    public long createChart(ChartCreateRequest chartCreateRequest, HttpServletRequest request) {
        // 1校验参数
        if (chartCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String name = chartCreateRequest.getName();
        String goal = chartCreateRequest.getGoal();
        String chartData = chartCreateRequest.getChartData();
        String chartType = chartCreateRequest.getChartType();
        if (StrUtil.hasBlank(goal, chartData, chartType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分析目标、图表数据、图表类型不能为空");
        }
        // 2获取当前登录用户，图表归属当前用户
        Long loginUserId = this.getLoginUserId(request);

        // 3插入数据库
        Chart newChart = new Chart();
        newChart.setName(name);
        newChart.setGoal(goal);
        newChart.setChartData(chartData);
        newChart.setChartType(chartType);
        newChart.setGenChart(chartCreateRequest.getGenChart());
        newChart.setGenResult(chartCreateRequest.getGenResult());
        newChart.setUserId(loginUserId);
        boolean result = this.save(newChart);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败");
        }
        return newChart.getId();
    }

    /**
     * 根据 id 修改自己的图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @param request            当前请求，用于获取登录态
     * @return 是否修改成功
     */
    @Override
    public boolean updateMyChart(ChartUpdateRequest chartUpdateRequest, HttpServletRequest request) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String name = chartUpdateRequest.getName();
        String goal = chartUpdateRequest.getGoal();
        String chartData = chartUpdateRequest.getChartData();
        String chartType = chartUpdateRequest.getChartType();
        if (StrUtil.hasBlank(goal, chartData, chartType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分析目标、图表数据、图表类型不能为空");
        }
        // 校验图表存在且属于当前用户
        Chart chart = this.getById(chartUpdateRequest.getId());
        this.checkChartOwner(chart, request);

        // 只更新允许修改的字段，创建用户不允许通过该接口变更
        Chart updateChart = new Chart();
        updateChart.setId(chartUpdateRequest.getId());
        updateChart.setName(name);
        updateChart.setGoal(goal);
        updateChart.setChartData(chartData);
        updateChart.setChartType(chartType);
        updateChart.setGenChart(chartUpdateRequest.getGenChart());
        updateChart.setGenResult(chartUpdateRequest.getGenResult());
        return this.updateById(updateChart);
    }

    /**
     * 根据 id 删除自己的图表
     *
     * @param id      图表 id
     * @param request 当前请求，用于获取登录态
     * @return 是否删除成功
     */
    @Override
    public boolean deleteMyChart(Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验图表存在且属于当前用户
        Chart chart = this.getById(id);
        this.checkChartOwner(chart, request);
        return this.removeById(id);
    }

    /**
     * 根据 id 查看自己的图表详情
     *
     * @param id      图表 id
     * @param request 当前请求，用于获取登录态
     * @return 图表详情
     */
    @Override
    public ChartVO getMyChartById(Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验图表存在且属于当前用户
        Chart chart = this.getById(id);
        this.checkChartOwner(chart, request);
        return this.getChartVO(chart);
    }

    /**
     * 分页查询自己的图表列表，每页最多 20 条，支持按图表名称查询
     *
     * @param chartQueryRequest 查询参数
     * @param request           当前请求，用于获取登录态
     * @return 分页后的图表列表
     */
    @Override
    public PageResultVO<ChartVO> listMyChartByPage(ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long loginUserId = this.getLoginUserId(request);
        // 强制按当前登录用户过滤，前端传入的 userId 无效
        QueryWrapper<Chart> queryWrapper = this.buildQueryWrapper(chartQueryRequest, loginUserId);
        // 用户端每页最多 20 条
        int current = chartQueryRequest.getCurrent() <= 0 ? 1 : chartQueryRequest.getCurrent();
        int pageSize = chartQueryRequest.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : chartQueryRequest.getPageSize();
        if (pageSize > USER_MAX_PAGE_SIZE) {
            pageSize = USER_MAX_PAGE_SIZE;
        }
        Page<Chart> page = this.page(new Page<>(current, pageSize), queryWrapper);
        List<ChartVO> chartVOList = page.getRecords().stream()
                .map(this::getChartVO)
                .collect(Collectors.toList());
        return new PageResultVO<>(chartVOList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 管理员根据 id 删除任意图表
     *
     * @param id 图表 id
     * @return 是否删除成功
     */
    @Override
    public boolean adminDeleteChart(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Chart chart = this.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        return this.removeById(id);
    }

    /**
     * 管理员根据 id 更新任意图表
     *
     * @param chartUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    @Override
    public boolean adminUpdateChart(ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String name = chartUpdateRequest.getName();
        String goal = chartUpdateRequest.getGoal();
        String chartData = chartUpdateRequest.getChartData();
        String chartType = chartUpdateRequest.getChartType();
        if (StrUtil.hasBlank(goal, chartData, chartType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分析目标、图表数据、图表类型不能为空");
        }
        Chart chart = this.getById(chartUpdateRequest.getId());
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        // 只更新允许修改的字段，创建用户不允许通过该接口变更
        Chart updateChart = new Chart();
        updateChart.setId(chartUpdateRequest.getId());
        updateChart.setName(name);
        updateChart.setGoal(goal);
        updateChart.setChartData(chartData);
        updateChart.setChartType(chartType);
        updateChart.setGenChart(chartUpdateRequest.getGenChart());
        updateChart.setGenResult(chartUpdateRequest.getGenResult());
        return this.updateById(updateChart);
    }

    /**
     * 管理员分页查询图表列表，支持按除时间外的任意字段查询，每页数量不限
     *
     * @param chartQueryRequest 查询参数
     * @return 分页后的图表列表
     */
    @Override
    public PageResultVO<ChartVO> adminListChart(ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<Chart> queryWrapper = this.getQueryWrapper(chartQueryRequest);
        // 管理员端不限制每页条数
        int current = chartQueryRequest.getCurrent() <= 0 ? 1 : chartQueryRequest.getCurrent();
        int pageSize = chartQueryRequest.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : chartQueryRequest.getPageSize();
        Page<Chart> page = this.page(new Page<>(current, pageSize), queryWrapper);
        List<ChartVO> chartVOList = page.getRecords().stream()
                .map(this::getChartVO)
                .collect(Collectors.toList());
        return new PageResultVO<>(chartVOList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 管理员根据 id 查看任意图表详情
     *
     * @param id 图表 id
     * @return 图表详情
     */
    @Override
    public ChartVO adminGetChartById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Chart chart = this.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        return this.getChartVO(chart);
    }

    /**
     * 将查询请求转为 QueryWrapper 查询条件，用于构造 SQL
     *
     * @param chartQueryRequest 查询参数
     * @return 查询条件
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        return this.buildQueryWrapper(chartQueryRequest, null);
    }

    /**
     * 构造查询条件，forceUserId 不为空时强制覆盖查询请求中的创建人
     *
     * @param chartQueryRequest 查询参数
     * @param forceUserId       强制指定的创建用户 id，为空时使用请求参数中的值
     * @return 查询条件
     */
    private QueryWrapper<Chart> buildQueryWrapper(ChartQueryRequest chartQueryRequest, Long forceUserId) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartData = chartQueryRequest.getChartData();
        String chartType = chartQueryRequest.getChartType();
        String genChart = chartQueryRequest.getGenChart();
        String genResult = chartQueryRequest.getGenResult();
        Long userId = forceUserId != null ? forceUserId : chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StrUtil.isNotBlank(chartData), "chartData", chartData);
        queryWrapper.like(StrUtil.isNotBlank(genChart), "genChart", genChart);
        queryWrapper.like(StrUtil.isNotBlank(genResult), "genResult", genResult);
        // 排序字段来自前端，必须走白名单校验，防止 SQL 注入
        if (StrUtil.isNotEmpty(sortField) && SORT_FIELDS.contains(sortField)) {
            // 常量在前，避免 sortOrder 为 null 时空指针
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(true, isAsc, sortField);
        }
        return queryWrapper;
    }

    /**
     * 获取当前登录用户 id
     *
     * @param request 当前请求，用于获取登录态
     * @return 当前登录用户 id
     */
    private Long getLoginUserId(HttpServletRequest request) {
        UserLoginVO loginUserVO = userService.getLoginUserVO(request);
        if (loginUserVO == null || loginUserVO.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUserVO.getId();
    }

    /**
     * 校验图表归属，图表不存在或不属于当前用户时抛出异常
     *
     * @param chart   图表实体
     * @param request 当前请求，用于获取登录态
     */
    private void checkChartOwner(Chart chart, HttpServletRequest request) {
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        Long loginUserId = this.getLoginUserId(request);
        if (!loginUserId.equals(chart.getUserId())) {
            log.info("chart access denied, chartId={} userId={}", chart.getId(), loginUserId);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该图表");
        }
    }

    /**
     * 把图表实体转换成视图对象
     *
     * @param chart 图表实体
     * @return 图表详情
     */
    private ChartVO getChartVO(Chart chart) {
        if (chart == null) {
            return null;
        }
        ChartVO chartVO = new ChartVO();
        chartVO.setId(chart.getId());
        chartVO.setName(chart.getName());
        chartVO.setGoal(chart.getGoal());
        chartVO.setChartData(chart.getChartData());
        chartVO.setChartType(chart.getChartType());
        chartVO.setGenChart(chart.getGenChart());
        chartVO.setGenResult(chart.getGenResult());
        chartVO.setUserId(chart.getUserId());
        chartVO.setCreateTime(chart.getCreateTime());
        chartVO.setUpdateTime(chart.getUpdateTime());
        return chartVO;
    }
}
