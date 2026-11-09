// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取某应用的对话历史（按创建时间降序，第 1 页为最新 10 条）GET /chat/history/list */
export async function listChatHistory(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listChatHistoryParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatMessage>('/chat/history/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 删除单条对话历史 POST /chat/history/delete */
export async function deleteChatHistory(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat/history/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员分页获取全部应用的对话历史（按时间降序）POST /chat/admin/list/page/vo */
export async function listChatHistoryByAdmin(
  body: API.ChatMessageQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatMessage>('/chat/admin/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
