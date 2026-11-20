-- 创建库
   create database if not exists ryo_picture;

        use ryo_picture;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;


-- 图表信息表
create table if not exists chart
(
    id           bigint auto_increment comment 'id' primary key,
    goal				 text null comment'分析目标',
    name              varchar(128) null comment '图表名称',
    chartData		 text null comment'图表数据',
    chartType		 varchar(128) null comment'图表类型',
    genChart		 text null comment'生成的图表数据',
    genResult		 text null comment'生成的分析结论',
    -- 任务状态字段(排队中wait、执行中running、已完成succeed、失败failed)
    status       varchar(128) not null default 'wait' comment 'wait,running,succeed,failed',
-- 任务执行信息字段
    execMessage  text   null comment '执行信息',

    userId			 bigint null comment'创建用户 id',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    INDEX idx_chart_userId (userId),
    INDEX idx_chart_name (name),
    INDEX idx_chart_goal (goal),
    INDEX idx_chart_chartType (chartType)
) comment '图表信息表' collate = utf8mb4_unicode_ci;

-- 已建表的库补索引（上面的 create table if not exists 对已存在的表不会生效），执行前先确认索引不存在
-- alter table chart add index idx_chart_userId (userId);
-- alter table chart add index idx_chart_name (name);
-- alter table chart add index idx_chart_goal (goal);
-- alter table chart add index idx_chart_chartType (chartType);
