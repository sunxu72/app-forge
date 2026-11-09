package com.sunxu.appforge.common;

import lombok.Data;

import java.io.Serializable;
/*删除请求类*/
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;//要删除的id

    private static final long serialVersionUID = 1L;//序列化版本号
}
