package io.onedev.server.web.component.issue.activities;

import com.google.common.collect.Lists;
import io.onedev.server.OneDev;
import io.onedev.server.attachment.AttachmentSupport;
import io.onedev.server.attachment.ProjectAttachmentSupport;
import io.onedev.server.entitymanager.IssueCommentManager;
import io.onedev.server.entityreference.ReferencedFromAware;
import io.onedev.server.model.*;
import io.onedev.server.model.support.issue.changedata.*;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.ProjectScopedCommit;
import io.onedev.server.web.ajaxlistener.ConfirmLeaveListener;
import io.onedev.server.web.behavior.ChangeObserver;
import io.onedev.server.web.component.comment.CommentInput;
import io.onedev.server.web.component.issue.activities.activity.*;
import io.onedev.server.web.component.user.ident.Mode;
import io.onedev.server.web.component.user.ident.UserIdentPanel;
import io.onedev.server.web.page.base.BasePage;
import io.onedev.server.web.page.simple.security.LoginPage;
import io.onedev.server.web.util.DeleteCallback;
import io.onedev.server.web.util.WicketUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;

import javax.servlet.http.Cookie;
import java.util.*;

import static io.onedev.server.security.SecurityUtils.canAccessTimeTracking;

public abstract class IssueActivitiesPanel extends Panel {

	private static final String COOKIE_SHOW_COMMENTS = "onedev.server.issue.showComments";
	
	private static final String COOKIE_SHOW_CHANGE_HISTORY = "onedev.server.issue.showChangeHistory";

	private static final String COOKIE_SHOW_REFERENCES = "onedev.server.issue.showReferences";
	
	private static final String COOKIE_SHOW_WORK_LOG = "onedev.server.issue.showWorkLog";
	
	private RepeatingView activitiesView;
	
	private Component showCommentsLink;

	private boolean showComments = true;
	
	private boolean showChangeHistory = true;
	
	private boolean showReferences = true;
	
	private boolean showWorkLog = true;
	
	public IssueActivitiesPanel(String panelId) {
		super(panelId);
		
		WebRequest request = (WebRequest) RequestCycle.get().getRequest();
		Cookie cookie = request.getCookie(COOKIE_SHOW_COMMENTS);
		if (cookie != null)
			showComments = Boolean.valueOf(cookie.getValue());
		
		cookie = request.getCookie(COOKIE_SHOW_CHANGE_HISTORY);
		if (cookie != null)
			showChangeHistory = Boolean.valueOf(cookie.getValue());

		cookie = request.getCookie(COOKIE_SHOW_REFERENCES);
		if (cookie != null)
			showReferences = Boolean.valueOf(cookie.getValue());
		
		cookie = request.getCookie(COOKIE_SHOW_WORK_LOG);
		if (cookie != null)
			showWorkLog = Boolean.valueOf(cookie.getValue());
	}

	@Override
	protected void onBeforeRender() {
		activitiesView = new RepeatingView("activities");
		addOrReplace(activitiesView);
		Issue issue = getIssue();

		var user = SecurityUtils.getAuthUser();
		for (IssueActivity activity: getActivities()) {
			if (issue.isVisitedAfter(activity.getDate())) {
				activitiesView.add(newActivityRow(activitiesView.newChildId(), activity));
			} else {
				Component row = newActivityRow(activitiesView.newChildId(), activity);
				if (user == null || !user.equals(activity.getUser()))
					row.add(AttributeAppender.append("class", "new"));
				activitiesView.add(row);
			}
		}		
		
		super.onBeforeRender();
	}
	
	private List<IssueActivity> getActivities() {
		List<IssueActivity> activities = new ArrayList<>();

		activities.add(new IssueOpenedActivity(getIssue()));

		List<IssueActivity> otherActivities = new ArrayList<>();
		
		if (showChangeHistory) {
			var project = getIssue().getProject();
			for (IssueChange change: getIssue().getChanges()) {
				if (!(change.getData() instanceof ReferencedFromAware) 
						&& !(change.getData() instanceof IssueReferencedFromCommitData)
						&& !(change.getData() instanceof IssueDescriptionChangeData)
						&& !(change.getData() instanceof IssueTotalEstimatedTimeChangeData)
						&& !(change.getData() instanceof IssueOwnSpentTimeChangeData)
						&& !(change.getData() instanceof IssueTotalSpentTimeChangeData)
						&& !(change.getData() instanceof IssueOwnEstimatedTimeChangeData && !canAccessTimeTracking(project))) {
					otherActivities.add(new IssueChangeActivity(change));
				}
			}
		}

		if (showReferences) {
			for (IssueChange change: getIssue().getChanges()) {
				if (change.getData() instanceof ReferencedFromAware) {
					ReferencedFromAware<?> referencedFromAware = (ReferencedFromAware<?>) change.getData();
					if (ReferencedFromAware.canDisplay(referencedFromAware))
						otherActivities.add(new IssueChangeActivity(change));
				} else if (change.getData() instanceof IssueReferencedFromCommitData) {
					ProjectScopedCommit commit = ((IssueReferencedFromCommitData) change.getData()).getCommit();
					if (commit.canDisplay())
						otherActivities.add(new IssueChangeActivity(change));
				}
			}
		}
		
		if (showComments) {
			for (IssueComment comment: getIssue().getComments())  
				otherActivities.add(new IssueCommentedActivity(comment));
		}
		
		if (showWorkLog && getIssue().getProject().isTimeTracking() 
				&& WicketUtils.isSubscriptionActive() 
				&& canAccessTimeTracking(getIssue().getProject())) {
			for (IssueWork work: getIssue().getWorks())
				otherActivities.add(new IssueWorkActivity(work));
		}
		
		otherActivities.sort(Comparator.comparingLong(o -> o.getDate().getTime()));
		
		activities.addAll(otherActivities);
		
		return activities;
	}
	
	private Component newActivityRow(String id, IssueActivity activity) {
		WebMarkupContainer row = new WebMarkupContainer(id, Model.of(activity));
		row.setOutputMarkupId(true);
		String anchor = activity.getAnchor();
		if (anchor != null)
			row.setMarkupId(anchor);
		
		if (activity.getUser() != null) {
			row.add(new UserIdentPanel("avatar", activity.getUser(), Mode.AVATAR));
			row.add(AttributeAppender.append("class", "with-avatar"));
		} else {
			row.add(new WebMarkupContainer("avatar").setVisible(false));
		}

		row.add(activity.render("content", (DeleteCallback) target -> {
			row.remove();
			target.appendJavaScript(String.format("$('#%s').remove();", row.getMarkupId()));
		}));
		
		row.add(AttributeAppender.append("class", activity.getClass().getSimpleName()));
		return row;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new ChangeObserver() {
			
			@Override
			public void onObservableChanged(IPartialPageRequestHandler handler, Collection<String> changedObservables) {
				updateActivities(handler);
			}
			
			private void updateActivities(IPartialPageRequestHandler handler) {
				@SuppressWarnings("deprecation")
				Component prevActivityRow = activitiesView.get(activitiesView.size()-1);
				IssueActivity lastActivity = (IssueActivity) prevActivityRow.getDefaultModelObject();
				List<IssueActivity> newActivities = new ArrayList<>();
				for (IssueActivity activity: getActivities()) {
					if (activity.getDate().getTime() > lastActivity.getDate().getTime()) 
						newActivities.add(activity);
				}
				
				var user = SecurityUtils.getAuthUser();
				for (IssueActivity activity: newActivities) {
					Component newActivityRow = newActivityRow(activitiesView.newChildId(), activity); 
					if (user == null || !user.equals(activity.getUser()))
						newActivityRow.add(AttributeAppender.append("class", "new"));
					activitiesView.add(newActivityRow);
					
					String script = String.format("$(\"<tr id='%s'></tr>\").insertAfter('#%s');", 
							newActivityRow.getMarkupId(), prevActivityRow.getMarkupId());
					handler.prependJavaScript(script);
					handler.add(newActivityRow);
					prevActivityRow = newActivityRow;
				}
			}
			
			@Override
			public Collection<String> findObservables() {
				return Lists.newArrayList(Issue.getDetailChangeObservable(getIssue().getId()));
			}
			
		});
		
		if (SecurityUtils.getAuthUser() != null) {
			Fragment fragment = new Fragment("addComment", "addCommentFrag", this);
			fragment.setOutputMarkupId(true);
			CommentInput input = new CommentInput("comment", Model.of(""), false) {

				@Override
				protected AttachmentSupport getAttachmentSupport() {
					return new ProjectAttachmentSupport(getProject(), getIssue().getUUID(), 
							SecurityUtils.canManageIssues(getProject()));
				}

				@Override
				protected Project getProject() {
					return getIssue().getProject();
				}
				
				@Override
				protected List<User> getParticipants() {
					return getIssue().getParticipants();
				}
				
				@Override
				protected List<Behavior> getInputBehaviors() {
					return Lists.newArrayList(AttributeModifier.replace("placeholder", "Leave a comment"));
				}

				@Override
				protected String getAutosaveKey() {
					return "issue:" + getIssue().getId() + ":new-comment";
				}
			};
			input.setOutputMarkupId(true);
			input.setRequired(true).setLabel(Model.of("Comment"));
			
			Form<?> form = new Form<Void>("form");
			form.add(new FencedFeedbackPanel("feedback", form));
			form.add(input);
			form.add(new AjaxSubmitLink("save") {

				@Override
				protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
					super.onSubmit(target, form);
					String content = input.getModelObject();
					if (content.length() > IssueComment.MAX_CONTENT_LEN) {
						error("Comment too long");
						target.add(form);
					} else {
						IssueComment comment = new IssueComment();
						comment.setContent(content);
						comment.setUser(SecurityUtils.getAuthUser());
						comment.setDate(new Date());
						comment.setIssue(getIssue());
						OneDev.getInstance(IssueCommentManager.class).create(comment);
						
						if (showComments) {
							((BasePage) getPage()).notifyObservablesChange(target, getIssue().getChangeObservables(false));
							target.add(fragment);
						} else {
							showComments = true;
							target.add(IssueActivitiesPanel.this);
							target.add(showCommentsLink);
						}
						input.clearMarkdown();
						input.focus(target);
					}
				}

				@Override
				protected void onError(AjaxRequestTarget target, Form<?> form) {
					super.onError(target, form);
					target.add(form);
				}
				
			});
			form.setOutputMarkupId(true);
			fragment.add(form);
			add(fragment);
		} else {
			Fragment fragment = new Fragment("addComment", "loginToCommentFrag", this);
			fragment.add(new AjaxLink<Void>("login") {

				@Override
				public void onClick(AjaxRequestTarget target) {
					throw new RestartResponseAtInterceptPageException(LoginPage.class);
				}
				
			});
			add(fragment);
		}
		
		setOutputMarkupId(true);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new IssueActivitiesCssResourceReference()));
	}

	protected abstract Issue getIssue();

	public Component renderOptions(String componentId) {
		Fragment fragment = new Fragment(componentId, "optionsFrag", this);
		fragment.add(showCommentsLink = new AjaxLink<Void>("showComments") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				showComments = !showComments;
				WebResponse response = (WebResponse) RequestCycle.get().getResponse();
				Cookie cookie = new Cookie(COOKIE_SHOW_COMMENTS, String.valueOf(showComments));
				cookie.setPath("/");
				cookie.setMaxAge(Integer.MAX_VALUE);
				response.addCookie(cookie);
				target.add(IssueActivitiesPanel.this);
				target.appendJavaScript(String.format("$('#%s').toggleClass('active');", getMarkupId()));
			}

		});
		showCommentsLink.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return showComments?"active":"";
			}
		}));
		
		fragment.add(new AjaxLink<Void>("showChangeHistory") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (showChangeHistory)
					add(AttributeAppender.append("class", "active"));
			}
			
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
			}
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				showChangeHistory = !showChangeHistory;
				WebResponse response = (WebResponse) RequestCycle.get().getResponse();
				Cookie cookie = new Cookie(COOKIE_SHOW_CHANGE_HISTORY, String.valueOf(showChangeHistory));
				cookie.setPath("/");
				cookie.setMaxAge(Integer.MAX_VALUE);
				response.addCookie(cookie);
				target.add(IssueActivitiesPanel.this);
				target.appendJavaScript(String.format("$('#%s').toggleClass('active');", getMarkupId()));
			}

		});

		fragment.add(new AjaxLink<Void>("showReferences") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (showReferences)
					add(AttributeAppender.append("class", "active"));
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				showReferences = !showReferences;
				WebResponse response = (WebResponse) RequestCycle.get().getResponse();
				Cookie cookie = new Cookie(COOKIE_SHOW_REFERENCES, String.valueOf(showReferences));
				cookie.setPath("/");
				cookie.setMaxAge(Integer.MAX_VALUE);
				response.addCookie(cookie);
				target.add(IssueActivitiesPanel.this);
				target.appendJavaScript(String.format("$('#%s').toggleClass('active');", getMarkupId()));
			}

		});
		
		fragment.add(new AjaxLink<Void>("showWorkLog") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (showWorkLog)
					add(AttributeAppender.append("class", "active"));
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				showWorkLog = !showWorkLog;
				WebResponse response = (WebResponse) RequestCycle.get().getResponse();
				Cookie cookie = new Cookie(COOKIE_SHOW_WORK_LOG, String.valueOf(showWorkLog));
				cookie.setPath("/");
				cookie.setMaxAge(Integer.MAX_VALUE);
				response.addCookie(cookie);
				target.add(IssueActivitiesPanel.this);
				target.appendJavaScript(String.format("$('#%s').toggleClass('active');", getMarkupId()));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				var project = getIssue().getProject();
				setVisible(project.isTimeTracking() 
						&& WicketUtils.isSubscriptionActive() 
						&& canAccessTimeTracking(project));
			}
			
		});
		
		return fragment;
	}
	
}
