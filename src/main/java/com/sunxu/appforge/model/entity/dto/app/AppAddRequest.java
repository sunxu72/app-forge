package com.sunxu.appforge.model.entity.dto.app;

import lombok.Data;

import java.io.Serializable;
/*
*   应用添加请求
* */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型：html / multi_file / vue_project
     */
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
