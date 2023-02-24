package org.codingmatters.tasks.support.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.ResponseDelegate;
import org.codingmatters.rest.io.Content;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskNotification;
import org.codingmatters.tasks.api.types.json.TaskNotificationWriter;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

public abstract class AbstractTaskHandler {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(AbstractTaskHandler.class);

    private final Supplier<TaskEntryPointAdapter> adapterProvider;
    private final JsonFactory jsonFactory;

    public AbstractTaskHandler(Supplier<TaskEntryPointAdapter> adapterProvider, JsonFactory jsonFactory) {
        this.adapterProvider = adapterProvider;
        this.jsonFactory = jsonFactory;
    }

    protected TaskEntryPointAdapter adapter() {
        return this.adapterProvider.get();
    }

    protected void notifyCallback(Task task, TaskNotification notification, Requester requester) {
        if(task.opt().status().run().isPresent()) {
            requester.header("status", task.status().run().name());
        }
        if(task.opt().status().exit().isPresent()) {
            requester.header("result", task.status().exit().name());
        }
        try {
            try (ByteArrayOutputStream json = new ByteArrayOutputStream(); JsonGenerator generator = this.jsonFactory.createGenerator(json)) {
                new TaskNotificationWriter().write(generator, notification);
                generator.flush();
                generator.close();
                ResponseDelegate response = requester.post("application/json", Content.from(json.toByteArray()));
                if(response.code() != 204) {
                    if(response.code() == 410) {
                        log.info("callback endpoint is gone");
                    } else {
                        log.error("callback endpoint failure : {}, {} - {}", response.code(), response.contentType(), response.body() != null ? new String(response.body()) : null);
                    }
                }
            }
        } catch(IOException e) {
            log.error("unexpected error notifying callback", e);
        }
    }
}
