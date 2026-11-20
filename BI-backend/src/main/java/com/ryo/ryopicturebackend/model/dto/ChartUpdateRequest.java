package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/*
* 图表修改请求*/
@Data
public class ChartUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1403462673149905641L;

    //图表 id
    private Long id;
    //图表名称
    private String name;
    //分析目标
    private String goal;
    //图表数据
    private String chartData;
    //图表类型
    private String chartType;
    //生成的图表数据
    private String genChart;
    //生成的分析结论
    private String genResult;
}
