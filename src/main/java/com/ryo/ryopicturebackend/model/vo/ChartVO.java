package com.ryo.ryopicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/*
* 图表详情视图对象*/
@Data
public class ChartVO implements Serializable {
    private static final long serialVersionUID = 1403462673149905643L;

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
    //任务状态：wait 排队中、running 执行中、succeed 已完成、failed 失败
    private String status;
    //任务执行信息，执行失败时记录失败原因
    private String execMessage;
    //创建用户 id
    private Long userId;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
}
