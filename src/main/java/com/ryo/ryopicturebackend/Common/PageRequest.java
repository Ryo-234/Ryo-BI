package com.ryo.ryopicturebackend.Common;

import lombok.Data;
/**
* 通用请求类 查询需要分页 就继承这个类 那个类就默认有了 页号 大小。。*/
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
