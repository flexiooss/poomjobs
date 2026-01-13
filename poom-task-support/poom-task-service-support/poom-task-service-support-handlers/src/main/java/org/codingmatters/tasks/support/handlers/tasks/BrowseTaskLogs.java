package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.tasks.api.TaskLogsGetRequest;
import org.codingmatters.tasks.api.TaskLogsGetResponse;
import org.codingmatters.tasks.api.tasklogsgetresponse.Status405;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;
import org.codingmatters.tasks.support.handlers.tasks.adapter.ReflectHandlerAdapter;
import org.codingmatters.tasks.support.handlers.tasks.adapter.UnadatableHandlerException;

import java.util.function.Function;
import java.util.function.Supplier;

public class BrowseTaskLogs extends AbstractTaskHandler implements Function<TaskLogsGetRequest, TaskLogsGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BrowseTaskLogs.class);

    private final int maxPageSize;

    public BrowseTaskLogs(Supplier<TaskEntryPointAdapter> adapterProvider, int maxPageSize, JsonFactory jsonFactory) {
        super(adapterProvider, jsonFactory);
        this.maxPageSize = maxPageSize;
    }

    public <Req, Resp> Function<Req, Resp> adapted(Class<Req> requestClass, Class<Resp> responseClass) {
        try {
            return new ReflectHandlerAdapter<>(this, requestClass, responseClass, TaskLogsGetRequest.class, TaskLogsGetResponse.class);
        } catch (UnadatableHandlerException e) {
            log.error("[GRAVE] error adapting adapter", e);
            throw new RuntimeException("error adapting handler", e);
        }
    }

    @Override
    public TaskLogsGetResponse apply(TaskLogsGetRequest request) {
        if (this.adapter().taskLogs().isPresent()) {
            Repository<TaskLog, PropertyQuery> repository = this.adapter().taskLogs().get();
            Rfc7233Pager.Page<TaskLog> page;
            try {
                page = Rfc7233Pager.forRequestedRange(request.range()).unit(TaskLog.class.getSimpleName()).maxPageSize(this.maxPageSize)
                        .pager(repository)
                        .page(this.query(request));
            } catch (RepositoryException e) {
                return TaskLogsGetResponse.builder().status500(st -> st.payload(Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("while browsing task logs, failed accessing repository", e))
                        .build())
                ).build();
            }

            if (!page.isValid()) {
                return TaskLogsGetResponse.builder().status416(st -> st.payload(Error.builder()
                        .code(Error.Code.BAD_REQUEST)
                        .token(log.tokenized().info("while browsing task logs, request not valid : {}", request))
                        .description(page.validationMessage())
                        .build())
                ).build();
            } else if (page.isPartial()) {
                return TaskLogsGetResponse.builder().status206(st -> st
                        .acceptRange(page.acceptRange()).contentRange(page.contentRange())
                        .payload(page.list().valueList())
                ).build();
            } else {
                return TaskLogsGetResponse.builder().status200(st -> st
                        .acceptRange(page.acceptRange()).contentRange(page.contentRange())
                        .payload(page.list().valueList())
                ).build();
            }
        } else {
            return TaskLogsGetResponse.builder().status405(Status405.builder().payload(Error.builder().code(Error.Code.COLLECTION_BROWSING_NOT_ALLOWED).build()).build()).build();
        }
    }

    private PropertyQuery query(TaskLogsGetRequest request) {
        PropertyQuery.Builder result = PropertyQuery.builder();
        if (request.opt().filter().isPresent() && !request.filter().trim().isEmpty()) {
            result.filter("taskId == '%s' && (%s)", request.taskId(), request.filter());
        } else {
            result.filter("taskId == '%s'", request.taskId());
        }
        return result.build();
    }
}
