package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenChartByAiRequest implements Serializable {
    /**
     * 业务
     */
    private String name;

    private String goal;

    private String chartType;
    private  static final long serialVersionUID = 1L;
}
