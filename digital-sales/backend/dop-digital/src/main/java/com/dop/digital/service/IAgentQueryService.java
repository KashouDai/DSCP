package com.dop.digital.service;

import com.dop.digital.agent.query.domain.AgentQueryPlan;
import com.dop.digital.agent.query.domain.AgentQueryResult;

public interface IAgentQueryService
{
    AgentQueryResult execute(AgentQueryPlan plan);
}
