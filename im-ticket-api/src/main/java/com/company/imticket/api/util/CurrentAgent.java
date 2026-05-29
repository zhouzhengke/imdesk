package com.company.imticket.api.util;

/**
 * Placeholder for extracting the current authenticated agent identity.
 * <p>
 * When Spring Security is integrated (Phase 2+), replace the implementation
 * with {@code SecurityContextHolder.getContext().getAuthentication()}.
 */
public final class CurrentAgent {

    private static final ThreadLocal<Long> agentIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> agentNameHolder = new ThreadLocal<>();

    private CurrentAgent() {}

    public static Long getAgentId() {
        Long id = agentIdHolder.get();
        if (id == null) {
            throw new IllegalStateException("No authenticated agent in context");
        }
        return id;
    }

    public static String getAgentName() {
        String name = agentNameHolder.get();
        return name != null ? name : "unknown";
    }

    public static void set(Long agentId, String agentName) {
        agentIdHolder.set(agentId);
        agentNameHolder.set(agentName);
    }

    public static void clear() {
        agentIdHolder.remove();
        agentNameHolder.remove();
    }
}
