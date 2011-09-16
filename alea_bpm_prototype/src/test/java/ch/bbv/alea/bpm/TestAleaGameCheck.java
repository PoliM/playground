package ch.bbv.alea.bpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.DefaultProcessEventListener;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

;
public class TestAleaGameCheck {

	@Mock
	private WorkflowService workflowService;

	private StatefulKnowledgeSession ksession;

	private TestWorkItemHandler workItemHandler;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		WorkflowServiceFactory.setInjector(Guice.createInjector(new AbstractModule() {

			@Override
			protected void configure() {
				bind(WorkflowService.class).toInstance(workflowService);
			}
		}));

		KnowledgeBase kbase = createKnowledgeBase();
		ksession = createKnowledgeSession(kbase);

		ksession.addEventListener(new DefaultProcessEventListener() {
			@Override
			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				System.out.println(event);
			}

			@Override
			public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
				System.out.print(event);
				System.out.println(event.getNodeInstance().getNodeName());
			}
		});

		workItemHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
	}

	@Test
	public void whenBadGameThenSendReject() throws Exception {
		// Arrange
		Map<String, Object> params = createProcessParameters(12345);

		when(workflowService.checkUploadedGame(anyLong())).thenReturn(false);

		// Act
		WorkflowProcessInstance processInstance = startProcess(ksession, params);

		// Assert
		verify(workflowService).checkUploadedGame(12345);
		verify(workflowService).sendBadUploadEMail(12345);
		verify(workflowService, never()).publishGame(anyLong());
		verify(workflowService, never()).sendGamePublishedEMail(anyLong());
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	@Test
	public void whenVipThenDirectPublish() throws Exception {
		// Arrange
		Map<String, Object> params = createProcessParameters(12345);

		when(workflowService.checkUploadedGame(12345)).thenReturn(true);
		when(workflowService.checkCustomerVip(12345)).thenReturn(true);

		// Act
		WorkflowProcessInstance processInstance = startProcess(ksession, params);

		// Assert
		verify(workflowService).checkUploadedGame(12345);
		verify(workflowService).publishGame(12345);
		verify(workflowService).sendGamePublishedEMail(12345);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	@Test
	public void whenNonVipAndGoodGameThenPublish() throws Exception {
		// Arrange
		Map<String, Object> params = createProcessParameters(12345);

		when(workflowService.checkUploadedGame(12345)).thenReturn(true);
		when(workflowService.checkCustomerVip(12345)).thenReturn(false);

		// Act
		WorkflowProcessInstance processInstance = startProcess(ksession, params);

		// Human Task step
		validateAndCompleteHumanTask_ManualGameCheck(Boolean.FALSE, "");

		// Assert
		verify(workflowService).checkUploadedGame(12345);
		verify(workflowService).publishGame(12345);
		verify(workflowService).sendGamePublishedEMail(12345);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	@Test
	public void whenNonVipAndBadGameThenReject() throws Exception {
		// Arrange
		Map<String, Object> params = createProcessParameters(12345);

		when(workflowService.checkUploadedGame(12345)).thenReturn(true);
		when(workflowService.checkCustomerVip(12345)).thenReturn(false);

		// Act
		WorkflowProcessInstance processInstance = startProcess(ksession, params);

		// Human Task step
		validateAndCompleteHumanTask_ManualGameCheck(Boolean.TRUE, "Contains bad stuf");

		// Assert
		verify(workflowService).checkUploadedGame(12345);
		verify(workflowService, never()).publishGame(anyLong());
		verify(workflowService, never()).sendGamePublishedEMail(anyLong());
		verify(workflowService).sendGameRejectedEMail(12345, "Contains bad stuf");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	private void validateAndCompleteHumanTask_ManualGameCheck(Boolean reject, String reason) {
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("GameValidators", workItem.getParameter("GroupId"));
		assertEquals(Long.valueOf(12345), workItem.getParameter("gameId"));
		// Complete step
		Map<String, Object> taskResult = new HashMap<String, Object>();
		taskResult.put("rejected", reject);
		taskResult.put("reason", reason);
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(), taskResult);
	}

	private WorkflowProcessInstance startProcess(StatefulKnowledgeSession ksession, Map<String, Object> params) {
		System.out.println("Starting process with parameters: " + params);
		return (WorkflowProcessInstance) ksession.startProcess("ch.bbv.alea.bpm.uploadValidation", params);
	}

	private Map<String, Object> createProcessParameters(int gameId) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("gameId", Long.valueOf(gameId));
		return params;
	}

	private KnowledgeBase createKnowledgeBase() throws Exception {
		System.out.println("Loading process... ");
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(ResourceFactory.newClassPathResource("alea.bpmn"), ResourceType.BPMN2);
		return kbuilder.newKnowledgeBase();
	}

	private StatefulKnowledgeSession createKnowledgeSession(KnowledgeBase kbase) {
		return kbase.newStatefulKnowledgeSession();
	}
}
