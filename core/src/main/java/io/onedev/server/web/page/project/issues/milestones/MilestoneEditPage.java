package io.onedev.server.web.page.project.issues.milestones;

import java.util.Date;

import org.apache.wicket.Session;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.manager.MilestoneManager;
import io.onedev.server.model.Milestone;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.editable.PathSegment;
import io.onedev.server.web.page.project.issues.IssuesPage;

@SuppressWarnings("serial")
public class MilestoneEditPage extends IssuesPage {

	private static final String PARAM_MILESTONE = "milestone";
	
	private final IModel<Milestone> milestoneModel;
	
	public MilestoneEditPage(PageParameters params) {
		super(params);
		
		Long milestoneId = params.get(PARAM_MILESTONE).toLong();
		milestoneModel = new LoadableDetachableModel<Milestone>() {

			@Override
			protected Milestone load() {
				return OneDev.getInstance(MilestoneManager.class).load(milestoneId);
			}
			
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		Milestone milestone = milestoneModel.getObject();
		BeanEditor editor = BeanContext.editBean("editor", milestone);
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();

				MilestoneManager milestoneManager = OneDev.getInstance(MilestoneManager.class);
				Milestone milestoneWithSameName = milestoneManager.find(getProject(), milestone.getName());
				if (milestoneWithSameName != null && !milestoneWithSameName.equals(milestone)) {
					editor.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another milestone in the project");
				} 
				if (!editor.hasErrors(true)){
					editor.getBeanDescriptor().copyProperties(milestone, milestoneModel.getObject());
					milestoneModel.getObject().setUpdateDate(new Date());
					milestoneManager.save(milestoneModel.getObject());
					Session.get().success("Milestone saved");
					setResponsePage(MilestoneDetailPage.class, MilestoneDetailPage.paramsOf(milestoneModel.getObject(), null));
				}
				
			}
			
		};
		form.add(editor);
		add(form);
	}
	
	@Override
	protected void onDetach() {
		milestoneModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new MilestonesResourceReference()));
	}

	public static PageParameters paramsOf(Milestone milestone) {
		PageParameters params = paramsOf(milestone.getProject());
		params.add(PARAM_MILESTONE, milestone.getId());
		return params;
	}
	
}
