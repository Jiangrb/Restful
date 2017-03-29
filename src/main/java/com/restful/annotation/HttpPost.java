package com.restful.annotation;

import com.restful.api.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP-POST
 * Created by SCWANG on 2016/6/12.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpPost {
    String value();
    HttpMethod method() default HttpMethod.POST;
}
