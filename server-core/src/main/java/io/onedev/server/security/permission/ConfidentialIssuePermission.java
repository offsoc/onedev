package io.onedev.server.security.permission;

import io.onedev.server.model.Issue;
import io.onedev.server.util.facade.UserFacade;
import org.apache.shiro.authz.Permission;
import org.jetbrains.annotations.Nullable;

public class ConfidentialIssuePermission implements BasePermission {

	private final Issue issue;
	
	public ConfidentialIssuePermission(Issue issue) {
		this.issue = issue;
	}
	
	@Override
	public boolean implies(Permission p) {
		if (p instanceof ConfidentialIssuePermission) {
			ConfidentialIssuePermission issuePermission = (ConfidentialIssuePermission) p;
			return issue.equals(issuePermission.issue);
		} else {
			return false;
		}
	}

	public Issue getIssue() {
		return issue;
	}

	@Override
	public boolean isApplicable(@Nullable UserFacade user) {
		return user != null;
	}
	
}
