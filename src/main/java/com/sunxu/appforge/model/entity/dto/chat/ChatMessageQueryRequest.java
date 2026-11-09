package com.sunxu.appforge.model.entity.dto.chat;

import com.sunxu.appforge.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话历史查询请求。
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessageQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 id
     */
    private Long id;

    /**
     * 所属应用 id
     */
    private Long appId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 消息类型（user / ai）
     */
    private String messageType;
}
