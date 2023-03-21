package me.zhengjie.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithOutToken {
    String key();

    /**
     * 防重复操作限时标记数值（存储redis限时标记数值）
     */
    String value() default "value";

    /**
     * 防重复操作过期时间（借助redis实现限时控制）
     */
    long expireSeconds() default 60;
}
