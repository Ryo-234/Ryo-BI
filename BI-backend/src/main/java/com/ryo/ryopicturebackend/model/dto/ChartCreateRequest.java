package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/*
* 创建图表请求*/
@Data
public class ChartCreateRequest implements Serializable {
    private static final long serialVersionUID = 1403462673149905640L;

    //图表名称
    private String name;
    //分析目标
    private String goal;
    //图表数据
    private String chartData;
    //图表类型
    private String chartType;
    //生成的图表数据，可选
    private String genChart;
    //生成的分析结论，可选
    private String genResult;
}
