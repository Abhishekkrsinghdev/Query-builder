package com.querybuilder.backend.shared.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querybuilder.backend.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect for auditing critical operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final ObjectMapper objectMapper;

    /**
     * Pointcut for create operations
     */
    @Pointcut("execution(* com.querybuilder.backend..service.*Service.create*(..))")
    public void createOperations() {}

    /**
     * Pointcut for update operations
     */
    @Pointcut("execution(* com.querybuilder.backend..service.*Service.update*(..))")
    public void updateOperations() {}

    /**
     * Pointcut for delete operations
     */
    @Pointcut("execution(* com.querybuilder.backend..service.*Service.delete*(..))")
    public void deleteOperations() {}

    /**
     * Audit create operations
     */
    @AfterReturning(pointcut = "createOperations()", returning = "result")
    public void auditCreate(JoinPoint joinPoint, Object result) {
        logAuditEvent("CREATE", joinPoint, result);
    }

    /**
     * Audit update operations
     */
    @AfterReturning(pointcut = "updateOperations()", returning = "result")
    public void auditUpdate(JoinPoint joinPoint, Object result) {
        logAuditEvent("UPDATE", joinPoint, result);
    }

    /**
     * Audit delete operations
     */
    @AfterReturning(pointcut = "deleteOperations()")
    public void auditDelete(JoinPoint joinPoint) {
        logAuditEvent("DELETE", joinPoint, null);
    }

    /**
     * Log audit event
     */
    private void logAuditEvent(String action, JoinPoint joinPoint, Object result) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("timestamp", LocalDateTime.now());
            auditLog.put("action", action);
            auditLog.put("method", joinPoint.getSignature().toShortString());
            auditLog.put("user", getCurrentUsername());
            auditLog.put("ipAddress", getClientIpAddress());
            auditLog.put("arguments", joinPoint.getArgs());

            if (result != null) {
                auditLog.put("resultType", result.getClass().getSimpleName());
            }

            String auditJson = objectMapper.writeValueAsString(auditLog);
            log.info("[AUDIT] {}", auditJson);

        } catch (JsonProcessingException e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getEmail();
            }
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Check for proxy headers
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }

            return ip;
        }

        return "unknown";
    }
}