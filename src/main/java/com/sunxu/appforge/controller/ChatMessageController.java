package com.sunxu.appforge.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.sunxu.appforge.annotation.AuthCheck;
import com.sunxu.appforge.common.BaseResponse;
import com.sunxu.appforge.common.DeleteRequest;
import com.sunxu.appforge.common.ResultUtils;
import com.sunxu.appforge.constant.UserConstant;
import com.sunxu.appforge.exception.BusinessException;
import com.sunxu.appforge.exception.ErrorCode;
import com.sunxu.appforge.exception.ThrowUtils;
import com.sunxu.appforge.model.entity.App;
import com.sunxu.appforge.model.entity.ChatMessage;
import com.sunxu.appforge.model.entity.User;
import com.sunxu.appforge.model.entity.dto.chat.ChatMessageQueryRequest;
import com.sunxu.appforge.service.AppService;
import com.sunxu.appforge.service.ChatMessageService;
import com.sunxu.appforge.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史控制层。
 *
 */
@RestController
@RequestMapping("/chat")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 分页获取某应用的对话历史（最新一页在前，前端每次加载最新 10 条并向前翻页）。
     * 仅应用创建者或管理员可见。
     *
     * @param appId   应用 id
     * @param pageNum 页码（第 1 页为最新 10 条）
     * @param pageSize 每页条数
     * @param request 请求对象
     * @return 对话历史分页（按创建时间降序）
     */
    @GetMapping("/history/list")
    public BaseResponse<Page<ChatMessage>> listChatHistory(@RequestParam Long appId,
                                                           @RequestParam(required = false, defaultValue = "1") long pageNum,
                                                           @RequestParam(required = false, defaultValue = "10") long pageSize,
                                                           HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 条");
        // 校验应用存在且当前用户有权访问（创建者或管理员）
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的对话历史");
        }
        // 按创建时间降序分页（第 1 页即最新 10 条）
        ChatMessageQueryRequest queryRequest = new ChatMessageQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setPageNum((int) pageNum);
        queryRequest.setPageSize((int) pageSize);
        QueryWrapper queryWrapper = chatMessageService.getQueryWrapper(queryRequest);
        return ResultUtils.success(chatMessageService.page(Page.of(pageNum, pageSize), queryWrapper));
    }

    /**
     * 删除单条对话历史（仅本人或管理员）
     *
     * @param deleteRequest 删除请求（含消息 id）
     * @param request       请求对象
     * @return 删除结果
     */
    @PostMapping("/history/delete")
    public BaseResponse<Boolean> deleteChatHistory(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChatMessage chatMessage = chatMessageService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(chatMessage == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        User loginUser = userService.getLoginUser(request);
        // 仅本人或管理员可删除
        if (!chatMessage.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chatMessageService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页获取全部应用的对话历史（按时间降序，便于内容监管）
     *
     * @param chatMessageQueryRequest 查询请求（可按 appId / userId / messageType 过滤）
     * @return 对话历史分页（按创建时间降序）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatMessage>> listChatHistoryByAdmin(@RequestBody ChatMessageQueryRequest chatMessageQueryRequest) {
        ThrowUtils.throwIf(chatMessageQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatMessageQueryRequest.getPageNum();
        long pageSize = chatMessageQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页最多查询 50 条");
        QueryWrapper queryWrapper = chatMessageService.getQueryWrapper(chatMessageQueryRequest);
        return ResultUtils.success(chatMessageService.page(Page.of(pageNum, pageSize), queryWrapper));
    }
}
