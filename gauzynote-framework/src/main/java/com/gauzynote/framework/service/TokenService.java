package com.gauzynote.framework.service;

import com.gauzynote.common.constant.CacheConstants;
import com.gauzynote.common.constant.Constants;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * token验证处理
 */
@Component
public class TokenService {
    private final static Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期
    @Value("${token.expireTime}")
    private int expireTime;

    // 一分钟的秒
    protected static final long MILLIS_MINUTE = 60 * 1000;
    // 20分钟
    private static final Long MILLIS_MINUTE_TWENTY = 20 * MILLIS_MINUTE;


    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    @Cacheable(value = "loginUserCache", key = "#uuid", unless = "#result == null")
    public LoginUser getLoginUser(String uuid) {
        if (!ObjectUtils.isEmpty(uuid)) {
            try {
                String userKey = getTokenKey(uuid);
                return (LoginUser) redisUtil.get(userKey);
            } catch (Exception e) {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 从请求中的token获取uuid
     */
    public String getUUIDByRequest(HttpServletRequest request) {
        try {
            String token = getToken(request);
            if (token == null) return null;
            Claims claims = parseToken(token);
            // 解析对应的权限以及用户信息
            return (String) claims.get(Constants.LOGIN_USER_KEY);
        } catch (Exception e) {
            log.error("获取token或者uuid异常'{}'", e.getMessage());
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser) {
        if (!ObjectUtils.isEmpty(loginUser) && !ObjectUtils.isEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    @CacheEvict(value = "loginUserCache", key = "#token")
    public void delLoginUser(String token) {
        if (!ObjectUtils.isEmpty(token)) {
            String userKey = getTokenKey(token);
            redisUtil.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌
     *
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser) {
        String token = UUID.randomUUID().toString();
        loginUser.setToken(token);
//        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        return createToken(claims);
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    @CachePut(value = "loginUserCache", key = "#loginUser.token")
    public void refreshToken(LoginUser loginUser) {
//        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(System.currentTimeMillis() + expireTime * MILLIS_MINUTE);

        String userKey = getTokenKey(loginUser.getToken());
        redisUtil.set(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     *
     * @param loginUser 登录信息
     */
    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY) {
            refreshToken(loginUser);
        }
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取请求token
     *
     * @return token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (!ObjectUtils.isEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }

        // 获取所有的 Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Auth-Token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        return token;
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    /**
     * 清空目标用户的所有登录 Token。
     * 在删除用户或停用用户时调用，强制该用户重新登录。
     *
     * @param userId 目标用户ID
     */
    public void clearUserTokens(Long userId) {
        Set<String> keys = redisUtil.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                try {
                    LoginUser loginUser = (LoginUser) redisUtil.get(key);
                    if (loginUser != null && userId.equals(loginUser.getUserId())) {
                        redisUtil.deleteObject(key);
                    }
                } catch (Exception e) {
                    log.error("清空用户登录信息异常 '{}'", e.getMessage());
                }
            }
        }
    }

}
