package com.sunxu.appforge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sunxu.appforge.exception.BusinessException;
import com.sunxu.appforge.exception.ErrorCode;
import com.sunxu.appforge.mapper.ChatMessageMapper;
import com.sunxu.appforge.model.entity.ChatMessage;
import com.sunxu.appforge.model.entity.dto.chat.ChatMessageQueryRequest;
import com.sunxu.appforge.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 对话历史服务层实现。
 *
 */
@Slf4j
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public QueryWrapper getQueryWrapper(ChatMessageQueryRequest chatMessageQueryRequest) {
        if (chatMessageQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = chatMessageQueryRequest.getId();
        Long appId = chatMessageQueryRequest.getAppId();
        Long userId = chatMessageQueryRequest.getUserId();
        String messageType = chatMessageQueryRequest.getMessageType();
        String sortField = chatMessageQueryRequest.getSortField();
        String sortOrder = chatMessageQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("messageType", StrUtil.isBlank(messageType) ? null : messageType)
                .orderBy(StrUtil.isBlank(sortField) ? "createTime" : sortField,
                        "ascend".equals(sortOrder));
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        try {
            // 逻辑删除该应用下的全部对话历史
            mapper.deleteByQuery(QueryWrapper.create().eq("appId", appId));
            return true;
        } catch (Exception e) {
            // 历史删除失败不阻断应用删除，仅记录日志
            log.error("删除应用 {} 的对话历史失败: {}", appId, e.getMessage());
            return false;
        }
    }
}
