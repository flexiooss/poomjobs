import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.test.TestRequesterFactory;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.demo.api.*;
import org.codingmatters.tasks.demo.api.types.TaskLogCreation;
import org.codingmatters.tasks.demo.api.types.TaskParam;
import org.codingmatters.tasks.demo.api.types.TaskResult;
import org.codingmatters.tasks.demo.api.types.TaskStatusChange;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.tasks.CreateTask;
import org.codingmatters.tasks.support.handlers.tasks.CreateTaskLog;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class TaskDemoHandlersBuilderTest {

    private final Repository<Task, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);

    private HandlerResource<JobCollectionPostRequest, JobCollectionPostResponse> jobCreationRequest = new HandlerResource<JobCollectionPostRequest, JobCollectionPostResponse>() {
        @Override
        protected JobCollectionPostResponse defaultResponse(JobCollectionPostRequest jobCollectionPostRequest) {
            return JobCollectionPostResponse.builder().status201(st -> st.xEntityId("created-job")).build();
        }
    };
    private final PoomjobsJobRegistryAPIClient jobsClient = new PoomjobsJobRegistryAPIHandlersClient(new PoomjobsJobRegistryAPIHandlers.Builder()
            .jobCollectionPostHandler(this.jobCreationRequest)
            .build(), Executors.newSingleThreadExecutor());

    private final TestRequesterFactory callbackRequesterFactory = new TestRequesterFactory(() -> "");
    private final AtomicReference<String> lastCallback = new AtomicReference<>();

    private final Supplier<TaskEntryPointAdapter> adapterSupplier = () -> new TaskEntryPointAdapter() {
        @Override
        public Repository<Task, PropertyQuery> tasks() {
            return repository;
        }

        @Override
        public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
            return Optional.empty();
        }

        @Override
        public JobSpec jobSpecFor(Task task) {
            return new JobSpec("test", "job", "task-url");
        }

        @Override
        public String jobAccount() {
            return "test-account";
        }

        @Override
        public Requester callbackRequester(String callbackUrl) {
            lastCallback.set(callbackUrl);
            return callbackRequesterFactory.create();
        }
    };


    @Test
    public void rootRoundtrip() throws Exception {
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);

        TaskDemoApiHandlers handlers = new TaskDemoHandlersBuilder(
                this.adapterSupplier,
                this.jobsClient,
                new JsonFactory()
        ).build();


        String taskId = handlers.rootCollectionPostHandler().apply(RootCollectionPostRequest.builder().callbackUrl("hey, how ya doing ?")
                .payload(TaskParam.builder().paramProp("param value").build())
                .build()).status201().xEntityId();
        System.out.println("Created TASK :: " + handlers.rootEntityGetHandler().apply(RootEntityGetRequest.builder().taskId(taskId).build()).status200().payload());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.rootStatusChangesPostHandler().apply(RootStatusChangesPostRequest.builder()
                .taskId(taskId)
                .payload(TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.rootLogsPostHandler().apply(RootLogsPostRequest.builder()
                .taskId(taskId)
                .payload(TaskLogCreation.builder().level(TaskLogCreation.Level.INFO).log("plop plop").build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.rootResultsPutHandler().apply(RootResultsPutRequest.builder()
                .taskId(taskId)
                .payload(TaskResult.builder().resultProp("result value").build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.rootStatusChangesPostHandler().apply(RootStatusChangesPostRequest.builder()
                .taskId(taskId)
                .payload(TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.SUCCESS).build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        System.out.println("Finished TASK :: " + handlers.rootEntityGetHandler().apply(RootEntityGetRequest.builder().taskId(taskId).build()).status200().payload());
    }

    @Test
    public void withParamRoundtrip() throws Exception {
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);
        this.callbackRequesterFactory.nextResponse(TestRequesterFactory.Method.POST, 204);

        TaskDemoApiHandlers handlers = new TaskDemoHandlersBuilder(this.adapterSupplier, this.jobsClient, new JsonFactory()).build();


        String taskId = handlers.taskWithParamCollectionPostHandler().apply(TaskWithParamCollectionPostRequest.builder().callbackUrl("hey, how ya doing ?")
                        .param("param 1")
                .payload(TaskParam.builder().paramProp("param value").build())
                .build()).status201().xEntityId();
        System.out.println("Created TASK :: " + handlers.taskWithParamEntityGetHandler().apply(TaskWithParamEntityGetRequest.builder().param("param 2").taskId(taskId).build()).status200().payload());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.taskWithParamStatusChangesPostHandler().apply(TaskWithParamStatusChangesPostRequest.builder()
                .param("param 3")
                .taskId(taskId)
                .payload(TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.taskWithParamLogsPostHandler().apply(TaskWithParamLogsPostRequest.builder()
                .param("param 4")
                .taskId(taskId)
                .payload(TaskLogCreation.builder().level(TaskLogCreation.Level.INFO).log("plop plop").build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.taskWithParamResultsPutHandler().apply(TaskWithParamResultsPutRequest.builder()
                .param("param 5")
                .taskId(taskId)
                .payload(TaskResult.builder().resultProp("result value").build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        handlers.taskWithParamStatusChangesPostHandler().apply(TaskWithParamStatusChangesPostRequest.builder()
                .param("param 5")
                .taskId(taskId)
                .payload(TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.SUCCESS).build())
                .build());
        System.out.println(" > " + new String(this.callbackRequesterFactory.lastCall().get().requestBody()));

        System.out.println("Finished TASK :: " + handlers.taskWithParamEntityGetHandler().apply(TaskWithParamEntityGetRequest.builder().taskId(taskId).param("param 6").build()).status200().payload());
    }
}