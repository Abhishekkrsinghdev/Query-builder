package com.querybuilder.backend.shared.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect for logging method executions
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    /**
     * Log before controller method execution
     */
    @Before("controllerMethods()")
    public void logBeforeController(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String username = getCurrentUsername();

        log.info("→ [CONTROLLER] {}.{} called by user: {} with args: {}",
                className, methodName, username, Arrays.toString(args));
    }

    /**
     * Log after controller method execution
     */
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterController(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("← [CONTROLLER] {}.{} completed successfully", className, methodName);
    }

    /**
     * Log controller exceptions
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logControllerException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.error("✗ [CONTROLLER] {}.{} threw exception: {}",
                className, methodName, exception.getMessage());
    }

    /**
     * Log service method execution time
     */
    @Around("serviceMethods()")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 1000) {  // Log slow operations (> 1 second)
                log.warn("⚠ [SERVICE] {}.{} took {}ms (SLOW)",
                        className, methodName, executionTime);
            } else {
                log.debug("[SERVICE] {}.{} executed in {}ms",
                        className, methodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("✗ [SERVICE] {}.{} failed after {}ms: {}",
                    className, methodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}