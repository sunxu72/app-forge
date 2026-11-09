package com.sunxu.appforge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sunxu.appforge.constant.AppConstant;
import com.sunxu.appforge.core.AiCodeGeneratorFacade;
import com.sunxu.appforge.exception.BusinessException;
import com.sunxu.appforge.exception.ErrorCode;
import com.sunxu.appforge.exception.ThrowUtils;
import com.sunxu.appforge.model.entity.App;
import com.sunxu.appforge.mapper.AppMapper;
import com.sunxu.appforge.model.entity.ChatMessage;
import com.sunxu.appforge.model.entity.User;
import com.sunxu.appforge.model.entity.dto.app.AppQueryRequest;
import com.sunxu.appforge.model.entity.enums.CodeGenTypeEnum;
import com.sunxu.appforge.model.entity.vo.AppVO;
import com.sunxu.appforge.model.entity.vo.UserVO;
import com.sunxu.appforge.service.AppService;
import com.sunxu.appforge.service.ChatMessageService;
import com.sunxu.appforge.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 应用部署后的访问地址，由配置注入，避免写死 localhost
     */
    @Value("${app.deploy.host:http://localhost:8123/api/static}")
    private String deployHost;
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 用户信息已批量查出，这里直接组装 VO，避免再走一次 getAppVO 触发 N+1 查询
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 保存用户消息到对话历史（失败不阻断生成，仅记录日志）
        try {
            ChatMessage userMessage = ChatMessage.builder()
                    .appId(appId)
                    .userId(loginUser.getId())
                    .messageType("user")
                    .content(message)
                    .status("success")
                    .build();
            chatMessageService.save(userMessage);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage());
        }
        // 6. 调用 AI 生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId, loginUser.getId());
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            File deployDir = new File(deployDirPath);
            // 先清空旧部署目录，避免新旧文件混杂
            FileUtil.del(deployDir);
            // Vue 项目：浏览器无法直接运行 .vue 源码，必须先构建生成 dist 再部署
            if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)) {
                buildVueProject(sourceDir);
                File distDir = new File(sourceDir, "dist");
                if (!distDir.exists() || !distDir.isDirectory()) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建产物不存在");
                }
                FileUtil.copyContent(distDir, deployDir, true);
            } else {
                FileUtil.copyContent(sourceDir, deployDir, true);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", deployHost, deployKey);
    }

    /**
     * 构建 Vue 项目（npm install + npm run build），生成 dist 产物
     *
     * @param projectDir Vue 项目源码目录
     */
    private void buildVueProject(File projectDir) {
        try {
            // 1. 安装依赖（如果 node_modules 不存在）
            File nodeModules = new File(projectDir, "node_modules");
            if (!nodeModules.exists() || !nodeModules.isDirectory()) {
                log.info("开始安装 Vue 项目依赖：npm install");
                int installExit = runCommand(projectDir, "npm install");
                if (installExit != 0) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "npm install 失败，退出码: " + installExit);
                }
            } else {
                log.info("检测到 node_modules 已存在，跳过 npm install");
            }
            // 2. 执行构建
            log.info("开始构建 Vue 项目：npm run build");
            int buildExit = runCommand(projectDir, "npm run build");
            if (buildExit != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "npm run build 失败，退出码: " + buildExit);
            }
            log.info("Vue 项目构建成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Vue 项目构建异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建异常：" + e.getMessage());
        }
    }

    /**
     * 在指定目录执行命令行命令（Windows），并等待完成
     *
     * @param workingDir 工作目录
     * @param command    要执行的命令
     * @return 退出码（0 表示成功）
     */
    private int runCommand(File workingDir, String command) throws Exception {
        // Windows 用 cmd，其它系统用 sh，否则部署到 Linux 会直接失败
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder pb = isWindows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("sh", "-c", command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // 读取输出流，避免缓冲区阻塞，同时记录日志
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[vue-build] {}", line);
            }
        }
        // 等待命令执行完成（最多 5 分钟）
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }



}
