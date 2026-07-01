package com.capteam.gaobackend.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Around("execution(public * com.capteam.gaobackend.service..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        long startedAt = System.currentTimeMillis();

        log.info("[SERVICE START] {}", methodName);

        try {
            Object result = joinPoint.proceed();
            log.info("[SERVICE END] {} durationMs={}", methodName, System.currentTimeMillis() - startedAt);
            return result;
        } catch (IllegalArgumentException | AccessDeniedException e) {
            log.warn("[SERVICE FAIL] {} durationMs={} message={}",
                    methodName,
                    System.currentTimeMillis() - startedAt,
                    e.getMessage()
            );
            throw e;
        } catch (Exception e) {
            log.error("[SERVICE ERROR] {} durationMs={}",
                    methodName,
                    System.currentTimeMillis() - startedAt,
                    e
            );
            throw e;
        }
    }
}
