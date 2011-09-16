package ch.bbv.alea.bpm;

import com.google.inject.AbstractModule;

public class WorkflowServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(WorkflowService.class).to(WorkflowServiceImpl.class);
	}
}
