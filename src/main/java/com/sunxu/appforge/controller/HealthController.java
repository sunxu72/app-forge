package com.sunxu.appforge.controller;

import com.sunxu.appforge.common.BaseResponse;
import com.sunxu.appforge.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping("/")//只接收Get请求
    public BaseResponse<String> healthCheck(){
        return ResultUtils.success("ok");
    }
}
