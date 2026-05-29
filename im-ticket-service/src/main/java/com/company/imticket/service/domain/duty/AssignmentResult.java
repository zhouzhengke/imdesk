package com.company.imticket.service.domain.duty;

/**
 * Value object representing the result of agent assignment.
 */
public class AssignmentResult {

    private final Long agentId;
    private final String agentName;
    private final String strategy;

    public AssignmentResult(Long agentId, String agentName, String strategy) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.strategy = strategy;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getStrategy() {
        return strategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentResult)) return false;
        AssignmentResult that = (AssignmentResult) o;
        return java.util.Objects.equals(agentId, that.agentId)
                && java.util.Objects.equals(agentName, that.agentName)
                && java.util.Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(agentId, agentName, strategy);
    }

    @Override
    public String toString() {
        return "AssignmentResult{agentId=" + agentId + ", agentName='" + agentName + "', strategy='" + strategy + "'}";
    }
}
