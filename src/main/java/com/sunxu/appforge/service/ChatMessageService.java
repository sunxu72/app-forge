package com.sunxu.appforge.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sunxu.appforge.model.entity.ChatMessage;
import com.sunxu.appforge.model.entity.dto.chat.ChatMessageQueryRequest;

/**
 * 对话历史服务层。
 *
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 构造对话历史查询条件（默认按创建时间降序）
     */
    QueryWrapper getQueryWrapper(ChatMessageQueryRequest chatMessageQueryRequest);

    /**
     * 删除某应用下的全部对话历史（逻辑删除，供删除应用时联动调用）
     *
     * @param appId 应用 id
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);
}
