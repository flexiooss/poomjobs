package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.tasks.api.TaskCollectionGetRequest;
import org.codingmatters.tasks.api.TaskCollectionGetResponse;
import org.codingmatters.tasks.api.taskcollectiongetresponse.Status200;
import org.codingmatters.tasks.api.taskcollectiongetresponse.Status206;
import org.codingmatters.tasks.api.taskcollectiongetresponse.Status416;
import org.codingmatters.tasks.api.taskcollectiongetresponse.Status500;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class BrowseTasks extends AbstractTaskHandler implements Function<TaskCollectionGetRequest, TaskCollectionGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BrowseTasks.class);
    private final int maxPageSize;

    public BrowseTasks(Supplier<TaskEntryPointAdapter> adapterProvider, int maxPageSize) {
        super(adapterProvider);
        this.maxPageSize = maxPageSize;
    }

    @Override
    public TaskCollectionGetResponse apply(TaskCollectionGetRequest request) {
        Repository<Task, PropertyQuery> repository = this.adapter().tasks();

        Rfc7233Pager.Page<Task> page;
        try {
            page = Rfc7233Pager.forRequestedRange(request.range())
                    .unit(Task.class.getName()).maxPageSize(this.maxPageSize)
                    .pager(repository).page(this.query(request));
        } catch (RepositoryException e) {
            return TaskCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("while browsing task, failed accessing repository", e))
                    .build()).build()).build();
        }

        if(! page.isValid()) {
            return TaskCollectionGetResponse.builder().status416(Status416.builder().payload(Error.builder()
                            .code(Error.Code.BAD_REQUEST)
                            .token(log.tokenized().info("while browsing task, request not valid : {}", request))
                            .description(page.validationMessage())
                    .build()).build()).build();
        } else if(page.isPartial()) {
            return TaskCollectionGetResponse.builder().status206(Status206.builder()
                            .acceptRange(page.acceptRange()).contentRange(page.contentRange())
                            .payload(page.list().valueList())
                    .build()).build();
        } else {
            return TaskCollectionGetResponse.builder().status200(Status200.builder()
                    .acceptRange(page.acceptRange()).contentRange(page.contentRange())
                    .payload(page.list().valueList())
                    .build()).build();
        }
    }

    private Optional<PropertyQuery> query(TaskCollectionGetRequest request) {
        if(request.opt().filter().isPresent() || request.opt().orderBy().isPresent()) {
            PropertyQuery.Builder result = PropertyQuery.builder();
            request.opt().filter().ifPresent(filter -> result.filter(filter));
            request.opt().orderBy().ifPresent(orderBy -> result.sort(orderBy));
            return Optional.of(result.build());
        } else {
            return Optional.empty();
        }
    }
}
