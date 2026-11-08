package com.ryo.ryopicturebackend.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举*/
@Getter
public enum UserRoleEnum {
    //如果枚举类多 for枚举类多了以后耗时间 不用for先把枚举值的 value和枚举类型 放到map中
    USER("用户","user"),
    ADMIN("管理员","admin");
    private  final String text;

    private final String value;
    //构造函数
    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    /**根据value获取枚举
     * @param value 枚举值的 value
     * @return 枚举值
    * */
    public static UserRoleEnum getEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for(UserRoleEnum e : UserRoleEnum.values()){
            if(e.value.equals(value)){
                return e;
            }
        }
        return null;
    }
}
