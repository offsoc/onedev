package io.onedev.server.search.entity.agent;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Agent;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.util.match.WildcardUtils;

public class IpAddressCriteria extends Criteria<Agent> {

	private static final long serialVersionUID = 1L;

	private final String value;
	
	private final int operator;
	
	public IpAddressCriteria(String value, int operator) {
		this.value = value;
		this.operator = operator;
	}

	@Override
	public Predicate getPredicate(CriteriaQuery<?> query, From<Agent, Agent> from, CriteriaBuilder builder) {
		Path<String> attribute = from.get(Agent.PROP_IP_ADDRESS);
		String normalized = value.toLowerCase().replace("*", "%");
		var predicate = builder.like(builder.lower(attribute), normalized);
		if (operator == AgentQueryLexer.IsNot)
			predicate = builder.not(predicate);
		return predicate;
	}

	@Override
	public boolean matches(Agent agent) {
		String name = agent.getName();
		var matches = name != null && WildcardUtils.matchString(value.toLowerCase(), name.toLowerCase());
		if (operator == AgentQueryLexer.IsNot)
			matches = !matches;
		return matches;
	}

	@Override
	public String toStringWithoutParens() {
		return quote(Agent.NAME_IP_ADDRESS) + " " 
				+ AgentQuery.getRuleName(operator) + " " 
				+ quote(value);
	}
	
}
