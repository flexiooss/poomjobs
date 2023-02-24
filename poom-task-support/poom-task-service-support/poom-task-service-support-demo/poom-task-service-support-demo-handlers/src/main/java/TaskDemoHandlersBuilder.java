import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.tasks.demo.api.*;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.tasks.*;

import java.util.function.Supplier;

public class TaskDemoHandlersBuilder extends TaskDemoApiHandlers.Builder {

    public TaskDemoHandlersBuilder(Supplier<TaskEntryPointAdapter> adapterSupplier, PoomjobsJobRegistryAPIClient jobs, JsonFactory jsonFactory) {
            this.configureRootHandler(adapterSupplier, jobs, jsonFactory);
            this.configureWithParamHandlers(adapterSupplier, jobs, jsonFactory);
    }

    private void configureRootHandler(Supplier<TaskEntryPointAdapter> adapterSupplier, PoomjobsJobRegistryAPIClient jobs, JsonFactory jsonFactory) {
        this.rootCollectionPostHandler(
                new CreateTask(adapterSupplier, jobs, jsonFactory).adapted(RootCollectionPostRequest.class, RootCollectionPostResponse.class)
        );
        this.rootCollectionGetHandler(
                new BrowseTasks(adapterSupplier, 100, jsonFactory).adapted(RootCollectionGetRequest.class, RootCollectionGetResponse.class)
        );
        this.rootEntityGetHandler(
                new RetrieveTask(adapterSupplier, jsonFactory).adapted(RootEntityGetRequest.class, RootEntityGetResponse.class)
        );
        this.rootStatusChangesPostHandler(
                new UpdateTaskStatus(adapterSupplier, jsonFactory).adapted(RootStatusChangesPostRequest.class, RootStatusChangesPostResponse.class)
        );
        this.rootLogsPostHandler(
                new CreateTaskLog(adapterSupplier, jsonFactory).adapted(RootLogsPostRequest.class, RootLogsPostResponse.class)
        );
        this.rootResultsPutHandler(
                new ReplaceTaskResult(adapterSupplier, jsonFactory).adapted(RootResultsPutRequest.class, RootResultsPutResponse.class)
        );
    }

    private void configureWithParamHandlers(Supplier<TaskEntryPointAdapter> adapterSupplier, PoomjobsJobRegistryAPIClient jobs, JsonFactory jsonFactory) {

        this.taskWithParamCollectionPostHandler(
                request -> new CreateTask(forParam(request.param(), adapterSupplier), jobs, jsonFactory).adapted(TaskWithParamCollectionPostRequest.class, TaskWithParamCollectionPostResponse.class)
                        .apply(request)
        );
        this.taskWithParamCollectionGetHandler(
                request -> new BrowseTasks(forParam(request.param(), adapterSupplier), 100, jsonFactory).adapted(TaskWithParamCollectionGetRequest.class, TaskWithParamCollectionGetResponse.class).apply(request)
        );
        this.taskWithParamEntityGetHandler(
                request -> new RetrieveTask(forParam(request.param(), adapterSupplier), jsonFactory).adapted(TaskWithParamEntityGetRequest.class, TaskWithParamEntityGetResponse.class).apply(request)
        );
        this.taskWithParamStatusChangesPostHandler(
                request -> new UpdateTaskStatus(forParam(request.param(), adapterSupplier), jsonFactory).adapted(TaskWithParamStatusChangesPostRequest.class, TaskWithParamStatusChangesPostResponse.class).apply(request)
        );
        this.taskWithParamLogsPostHandler(
                request -> new CreateTaskLog(forParam(request.param(), adapterSupplier), jsonFactory).adapted(TaskWithParamLogsPostRequest.class, TaskWithParamLogsPostResponse.class).apply(request)
        );
        this.taskWithParamResultsPutHandler(
                request -> new ReplaceTaskResult(forParam(request.param(), adapterSupplier), jsonFactory).adapted(TaskWithParamResultsPutRequest.class, TaskWithParamResultsPutResponse.class).apply(request)
        );
    }

    private Supplier<TaskEntryPointAdapter> forParam(String param, Supplier<TaskEntryPointAdapter> adapterSupplier) {
        System.out.println("!! adapter supplier configured for param : " + param);
        return adapterSupplier;
    }
}
