package io.onedev.server.buildspec.job.retrycondition;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import io.onedev.server.util.ProjectScope;
import io.onedev.server.util.criteria.Criteria;

public class ParamCriteria extends Criteria<RetryContext> {

	private static final long serialVersionUID = 1L;

	private final String name;
	
	private final String value;
	
	private final int operator;
	
	public ParamCriteria(String name, String value, int operator) {
		this.name = name;
		this.value = value;
		this.operator = operator;
	}

	@Override
	public Predicate getPredicate(@Nullable ProjectScope projectScope, CriteriaQuery<?> query, From<RetryContext, RetryContext> from, CriteriaBuilder builder) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean matches(RetryContext context) {
		List<String> paramValues = context.getBuild().getParamMap().get(name);
		var matches = paramValues != null && paramValues.contains(value);
		if (operator == RetryConditionLexer.IsNot)
			matches = !matches;
		return matches;
	}

	@Override
	public String toStringWithoutParens() {
		return quote(name) + " " 
				+ RetryCondition.getRuleName(operator) + " "
				+ quote(value);
	}
	
}
