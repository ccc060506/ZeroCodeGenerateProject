package com.ccc.zerocodegenerateproject.interceptor;

import com.ccc.zerocodegenerateproject.common.util.JwtUtil;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 不是方法直接放行（静态资源）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(response, "未登录，缺少Token");
        }

        token = token.replace("Bearer ", "");

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(response, "登录已过期或Token无效");
        }

        String redisKey = "token:" + token;
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (!hasKey) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"Token已失效，请重新登录\"}");
            return false;
        }

        // Token 有效 → 刷新过期时间
        Long remaining = redisTemplate.getExpire(redisKey, TimeUnit.DAYS);
        if(remaining != null && remaining < 3){
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
        }
        // 解析并存储用户信息
        Claims claims = jwtUtil.parseToken(token);
        Long id = claims.get("id", Long.class);
        String username = claims.get("username", String.class);
        UserContext.setCurrentUser(id,username);

        System.out.println("接收到的Token: " + token);
        try {
            boolean isValid = jwtUtil.validateToken(token);
            System.out.println("Token是否有效: " + isValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理线程变量
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response, String msg)
            throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":401,\"msg\":\"%s\"}", msg)
        );
        return false;
    }
}