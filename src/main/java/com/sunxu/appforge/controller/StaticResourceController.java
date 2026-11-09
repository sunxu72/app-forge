package com.sunxu.appforge.controller;

import com.sunxu.appforge.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

/**
 * 静态资源访问控制器，用于预览生成的代码产物
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 部署文件根目录（读取已部署到 code_deploy 的产物）
    private static final String DEPLOY_ROOT_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;
    // 生成文件根目录（读取未部署的生成产物，预览使用）
    private static final String OUTPUT_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 提供静态资源访问，支持目录自动回退到 index.html
     * 访问格式：http://localhost:8123/api/static/{deployKey}/{fileName}
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取真正的资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());

            // 依次从部署目录、生成目录查找（预览路径 codeGenType_appId 存在于生成目录）
            for (String rootDir : new String[]{DEPLOY_ROOT_DIR, OUTPUT_ROOT_DIR}) {
                File targetFile = new File(rootDir, deployKey + resourcePath);

                // 防路径穿越：解析后的路径必须仍在根目录内
                File rootFile = new File(rootDir);
                if (!targetFile.getCanonicalPath().startsWith(rootFile.getCanonicalPath() + File.separator)
                        && !targetFile.getCanonicalPath().equals(rootFile.getCanonicalPath())) {
                    continue;
                }

                // 目录访问（或路径为空）时，自动回退到该目录下的 index.html
                if (!targetFile.exists() || targetFile.isDirectory()) {
                    if (targetFile.isDirectory() || resourcePath.isEmpty()) {
                        File indexFile = new File(rootDir, deployKey + resourcePath + File.separator + "index.html");
                        if (indexFile.exists() && indexFile.isFile()) {
                            return ResponseEntity.ok(new FileSystemResource(indexFile));
                        }
                    }
                    continue;
                }

                return ResponseEntity.ok(new FileSystemResource(targetFile));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
