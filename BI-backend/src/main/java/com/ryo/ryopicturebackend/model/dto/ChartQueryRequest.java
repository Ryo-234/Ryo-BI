package com.ryo.ryopicturebackend.model.dto;

import com.ryo.ryopicturebackend.Common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
* 图表列表查询请求，继承通用分页请求类，额外支持按条件筛选
* 时间字段（创建时间、更新时间）不作为查询条件*/
@Data
@EqualsAndHashCode(callSuper = true)
public class ChartQueryRequest extends PageRequest {
    private static final long serialVersionUID = 1403462673149905642L;

    //图表 id，精确查询
    private Long id;
    //图表名称，模糊查询
    private String name;
    //分析目标，模糊查询
    private String goal;
    //图表数据，模糊查询
    private String chartData;
    //图表类型，精确查询
    private String chartType;
    //生成的图表数据，模糊查询
    private String genChart;
    //生成的分析结论，模糊查询
    private String genResult;
    //创建用户 id，精确查询
    private Long userId;
}
