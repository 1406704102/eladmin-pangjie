package me.zhengjie.aspect;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.Ipv4Util;
import me.zhengjie.annotation.WithOutToken;
import me.zhengjie.utils.DateUtil;
import me.zhengjie.utils.Md5Util;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class WithOutTokenAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 定义切点
     */
    @Pointcut("@annotation(me.zhengjie.annotation.WithOutToken)")
    public void withOutToken() {
    }

    @Around("withOutToken()")
    public Object before(ProceedingJoinPoint joinPoint) throws Exception {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();


        Assert.notNull(request, "request cannot be null.");

        //获取执行方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        //获取防重复提交注解
        WithOutToken annotation = method.getAnnotation(WithOutToken.class);

        String timestamp = request.getHeader("Timestamp");
        Integer timeBetweenSeconds = DateUtil.getTimeBetweenSeconds(new Timestamp(Long.parseLong(timestamp)), Timestamp.from(Instant.now()));
        if (timeBetweenSeconds >= 5) {
            throw new RuntimeException("4000");
        }
        String sign = request.getHeader("Sign").toUpperCase();
        String s = Md5Util.MD5("BuYaoLaiShuaLeMaDe" + timestamp + "pangjie");
        if (!s.equals(sign.toUpperCase())) {
            throw new RuntimeException("4000");
        }
        String key = annotation.key();

        String redisKey = "PREVENT_DUPLICATION_PREFIX:"
                .concat(sign).concat(key);
        String redisValue = redisKey.concat(annotation.value()).concat("submit duplication");

        if (!redisTemplate.hasKey(redisKey)) {
            //设置防重复操作限时标记（前置通知）
            redisTemplate.opsForValue()
                    .set(redisKey, redisValue, annotation.expireSeconds(), TimeUnit.SECONDS);
            try {
                //正常执行方法并返回
                //ProceedingJoinPoint类型参数可以决定是否执行目标方法，且环绕通知必须要有返回值，返回值即为目标方法的返回值
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                //确保方法执行异常实时释放限时标记(异常后置通知)
                redisTemplate.delete(redisKey);
                throw new RuntimeException(throwable);
            }
        } else {
            throw new RuntimeException("4000");
        }
    }

}
