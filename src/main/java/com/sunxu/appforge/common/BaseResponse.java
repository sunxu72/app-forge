package com.sunxu.appforge.common;

import com.sunxu.appforge.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
//统一返回包装类格，统一返回格式
@Data//是 Lombok 注解：自动生成 getter/setter/toString，不用你手写
public class BaseResponse<T> implements Serializable {//Serializable序列化接口，以后存 Redis 或者传输的时候要用

    private int code;//返回的状态码

    private T data;//返回前端的数据

    private String message;//返回提示的信息

    public BaseResponse(int code, T data, String message) {//构造函数
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }//**构造重载**，只传 code 和 data，不用写 message

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());//接受自定义枚举返回统一格式
    }
}
