package io.onedev.server.web.page.admin.usermanagement;

import static io.onedev.server.web.translation.Translation._T;

import java.text.MessageFormat;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.AuditManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.entitymanager.UserInvitationManager;
import io.onedev.server.model.UserInvitation;
import io.onedev.server.web.component.taskbutton.TaskButton;
import io.onedev.server.web.component.taskbutton.TaskResult;
import io.onedev.server.web.component.taskbutton.TaskResult.PlainMessage;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.page.admin.AdministrationPage;
import io.onedev.server.web.page.admin.mailservice.MailServicePage;

public class NewInvitationPage extends AdministrationPage {

	public NewInvitationPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		var mailService = OneDev.getInstance(SettingManager.class).getMailService();
		if (mailService != null) {
			Fragment fragment = new Fragment("content", "inviteFrag", this);
			NewInvitationBean bean = new NewInvitationBean();
			Form<?> form = new Form<Void>("form");
			form.add(BeanContext.edit("editor", bean));
			form.add(new FencedFeedbackPanel("feedback", form));
			form.add(new TaskButton("invite") {

				@Override
				protected TaskResult runTask(TaskLogger logger) throws InterruptedException {
					for (String emailAddress: bean.getListOfEmailAddresses()) {
						logger.log(MessageFormat.format(_T("Sending invitation to \"{0}\"..."), emailAddress));
						UserInvitation invitation = new UserInvitation();
						invitation.setEmailAddress(emailAddress);
						UserInvitationManager userInvitationManager = OneDev.getInstance(UserInvitationManager.class);
						userInvitationManager.create(invitation);
						OneDev.getInstance(AuditManager.class).audit(null, "created invitation for \"" + emailAddress + "\"", null, null);
						userInvitationManager.sendInvitationEmail(invitation);
						if (Thread.interrupted())
							throw new InterruptedException();
					}
					return new TaskResult(true, new PlainMessage(_T("Invitations sent")));
				}

				@Override
				protected void onCompleted(AjaxRequestTarget target, boolean successful) {
					if (successful)
						setResponsePage(InvitationListPage.class);
				}
				
			});
			fragment.add(form);
			add(fragment);
		} else {
			Fragment fragment = new Fragment("content", "noMailServiceFrag", this);
			fragment.add(new BookmarkablePageLink<Void>("mailSetting", MailServicePage.class));
			add(fragment);
		}
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new Label(componentId, _T("Invite Users"));
	}

}
