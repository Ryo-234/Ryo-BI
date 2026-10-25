package com.ryo.ryopicturebackend.Controller;

import com.ryo.ryopicturebackend.Common.BaseResponse;
import com.ryo.ryopicturebackend.Common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController//声明这是个接口
@RequestMapping("/")//声明接口的路径
public class MainController {
    /**
     * 健康检查
     * @return*/
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");

    }
}

