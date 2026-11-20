package com.ryo.ryopicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/*
* 分页结果视图对象*/
@Data
public class PageResultVO<T> implements Serializable {
    private static final long serialVersionUID = 1403462673149905637L;

    //当前页数据
    private List<T> records;
    //总记录数
    private long total;
    //当前页号
    private long current;
    //页面大小
    private long pageSize;
    //总页数
    private long pages;

    public PageResultVO(List<T> records, long total, long current, long pageSize) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.pageSize = pageSize;
        this.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }
}
