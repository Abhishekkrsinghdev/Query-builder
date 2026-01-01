package com.querybuilder.backend.shared.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for monitoring performance of critical operations
 */
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    /**
     * Pointcut for query execution methods
     */
    @Pointcut("execution(* com.querybuilder.backend.query.service.QueryExecutionService.execute*(..))")
    public void queryExecutionMethods() {}

    /**
     * Pointcut for database operations
     */
    @Pointcut("execution(* com.querybuilder.backend.datasource.service.*.*(..))")
    public void datasourceOperations() {}

    /**
     * Monitor query execution performance
     */
    @Around("queryExecutionMethods()")
    public Object monitorQueryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        log.info("⏱ [PERFORMANCE] Starting query execution: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 5000) {
                log.warn("⚠ [PERFORMANCE] SLOW QUERY: {} took {}ms", methodName, executionTime);
            } else if (executionTime > 2000) {
                log.warn("⚡ [PERFORMANCE] {} took {}ms", methodName, executionTime);
            } else {
                log.info("✓ [PERFORMANCE] {} completed in {}ms", methodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("✗ [PERFORMANCE] {} failed after {}ms", methodName, executionTime);
            throw throwable;
        }
    }

    /**
     * Monitor datasource operations
     */
    @Around("datasourceOperations()")
    public Object monitorDatasourceOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 3000) {
                log.warn("⚠ [DATASOURCE] SLOW: {}.{} took {}ms",
                        className, methodName, executionTime);
            } else {
                log.debug("[DATASOURCE] {}.{} completed in {}ms",
                        className, methodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("✗ [DATASOURCE] {}.{} failed after {}ms: {}",
                    className, methodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }
}