package com.gauzynote.common.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP响应工具类，用于简化HttpServletResponse的操作
 */
public class ResponseUtils {

    // 禁止实例化
    private ResponseUtils() {}

    /**
     * 设置JSON响应内容
     * @param response HttpServletResponse对象
     * @param status  HTTP状态码
     * @param content JSON内容
     */
    public static void writeJson(HttpServletResponse response, int status, String content) {
        setCommonHeaders(response, "application/json;charset=UTF-8");
        response.setStatus(status);
        writeContent(response, content);
    }

    /**
     * 设置文本响应内容
     * @param response HttpServletResponse对象
     * @param status  HTTP状态码
     * @param content 文本内容
     */
    public static void writeText(HttpServletResponse response, int status, String content) {
        setCommonHeaders(response, "text/plain;charset=UTF-8");
        response.setStatus(status);
        writeContent(response, content);
    }

    /**
     * 设置HTML响应内容
     * @param response HttpServletResponse对象
     * @param status  HTTP状态码
     * @param content HTML内容
     */
    public static void writeHtml(HttpServletResponse response, int status, String content) {
        setCommonHeaders(response, "text/html;charset=UTF-8");
        response.setStatus(status);
        writeContent(response, content);
    }

    /**
     * 设置重定向
     * @param response HttpServletResponse对象
     * @param url      重定向URL
     */
    public static void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            throw new RuntimeException("重定向失败: " + url, e);
        }
    }

    /**
     * 设置永久重定向
     * @param response HttpServletResponse对象
     * @param url      重定向URL
     */
    public static void redirectPermanent(HttpServletResponse response, String url) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", url);
    }

    /**
     * 设置错误响应
     * @param response HttpServletResponse对象
     * @param status   HTTP错误状态码
     * @param message  错误消息
     */
    public static void sendError(HttpServletResponse response, int status, String message) {
        try {
            response.sendError(status, message);
        } catch (IOException e) {
            throw new RuntimeException("发送错误响应失败", e);
        }
    }

    /**
     * 添加Cookie
     * @param response HttpServletResponse对象
     * @param name     Cookie名称
     * @param value    Cookie值
     * @param maxAge   有效期（秒）
     * @param path     路径
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        try {
            Cookie cookie = new Cookie(name, URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            cookie.setPath(path);
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);
        } catch (Exception e) {
            throw new RuntimeException("设置Cookie失败", e);
        }
    }

    /**
     * 设置响应头
     * @param response HttpServletResponse对象
     * @param headers  响应头键值对
     */
    public static void setHeaders(HttpServletResponse response, Map<String, String> headers) {
        headers.forEach(response::setHeader);
    }

    /**
     * 设置下载文件响应头
     * @param response  HttpServletResponse对象
     * @param fileName  文件名
     * @param contentType 内容类型（如 application/octet-stream）
     */
    public static void setDownloadHeaders(HttpServletResponse response, String fileName, String contentType) {
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    // 私有工具方法
    private static void setCommonHeaders(HttpServletResponse response, String contentType) {
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
//        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
//        response.setHeader("Pragma", "no-cache");
//        response.setDateHeader("Expires", 0);
    }

    private static void writeContent(HttpServletResponse response, String content) {
        try (PrintWriter writer = response.getWriter()) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("写入响应内容失败", e);
        }
    }
}