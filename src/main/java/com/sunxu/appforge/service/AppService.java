package com.sunxu.appforge.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sunxu.appforge.exception.BusinessException;
import com.sunxu.appforge.exception.ErrorCode;
import com.sunxu.appforge.model.entity.App;
import com.sunxu.appforge.model.entity.User;
import com.sunxu.appforge.model.entity.dto.app.AppQueryRequest;
import com.sunxu.appforge.model.entity.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 *  服务层。
 *
 */
public interface AppService extends IService<App> {
    /*
     * 根据 App 对象获取 AppVO 对象
    **/
    AppVO getAppVO(App app);
/*
* 获取应用封装类列表
* */
    List<AppVO> getAppVOList(List<App> appList);

    /*
    * 构造应用查询条件
    * */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);
    /*
    * 网站部署
    * */
    String deployApp(Long appId, User loginUser);
}
