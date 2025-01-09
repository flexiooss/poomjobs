package org.codingmatters.tasks.support.jobs;

import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.tasks.api.*;
import org.codingmatters.tasks.api.taskentitygetresponse.Status200;
import org.codingmatters.tasks.api.taskstatuschangespostresponse.Status201;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.client.TaskApiClient;
import org.codingmatters.tasks.client.TaskApiHandlersClient;
import org.codingmatters.value.objects.demo.books.Book;
import org.codingmatters.value.objects.demo.books.Person;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class TaskJobProcessorTest {

    private final AtomicReference<String> retrievedTask = new AtomicReference<>();
    private final List<TaskStatusChange> statusChanges = Collections.synchronizedList(new LinkedList<>());
    private final List<TaskLogCreation> logs = Collections.synchronizedList(new LinkedList<>());
    private final AtomicReference<ObjectValue> result = new AtomicReference<>();

    private TaskApiClient taskClient = new TaskApiHandlersClient(new TaskApiHandlers.Builder()
            .taskEntityGetHandler(request -> {
                retrievedTask.set(request.taskId());
                return TaskEntityGetResponse.builder().status200(Status200.builder().xEntityId(request.taskId())
                        .payload(Task.builder().id(request.taskId()).params(ObjectValue.fromMap(Person.builder()
                                .name("author name")
                                .build().toMap()).build()).build())
                        .build()).build();
            })
            .taskStatusChangesPostHandler(request -> {
                statusChanges.add(request.payload());
                return TaskStatusChangesPostResponse.builder().status201(Status201.builder().build()).build();
            })
            .taskLogsPostHandler(request -> {
                logs.add(request.payload());
                return TaskLogsPostResponse.builder().status201(org.codingmatters.tasks.api.tasklogspostresponse.Status201.builder().build()).build();
            })
            .taskResultsPutHandler(request -> {
                result.set(request.payload());
                return TaskResultsPutResponse.builder().status200(org.codingmatters.tasks.api.taskresultsputresponse.Status200.builder().build()).build();
            })
            .build(), Executors.newSingleThreadExecutor());

    @Test
    public void whenTaskSucceeds__thenNominalStatusChanges_andResultSet_andJobHasSuccessStatus() throws Exception {
        TaskJobProcessor<Person, Book> processor = new TaskJobProcessor<>(Job.builder()
                .arguments("task-id", "task-url")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                return (id, person, taskNotifier, args) -> Book.builder().author(person).build();
            }
        };

        Job job = processor.process();

        assertThat(this.retrievedTask.get(), is("task-id"));
        assertThat(this.statusChanges, contains(
                TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build(),
                TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.SUCCESS).build()
        ));
        assertThat(this.result.get().toMap(), is(Book.builder().author(Person.builder().name("author name").build()).build().toMap()));
        assertThat(this.logs, is(empty()));
        assertThat(job.status(), is(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()));
    }

    @Test
    public void whenTaskProcessorLogs__thenLogNotificated() throws Exception {
        new TaskJobProcessor<>(Job.builder()
                .arguments("task-id", "task-url")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                return (id, person, taskNotifier, args) -> {
                    taskNotifier.info("that's an info");
                    taskNotifier.error("that's an error");
                    return Book.builder().author(person).build();
                };
            }
        }.process();

        assertThat(this.logs, contains(
                TaskLogCreation.builder().level(TaskLogCreation.Level.INFO).log("that's an info").build(),
                TaskLogCreation.builder().level(TaskLogCreation.Level.ERROR).log("that's an error").build()
        ));
    }

    @Test
    public void whenTaskProcessorFails__thenStatusChangesToFAILURE_andJobHasFailureStatus() throws Exception {
        TaskJobProcessor<Person, Book> processor = new TaskJobProcessor<>(Job.builder()
                .arguments("task-id", "task-url")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                return (id, person, taskNotifier, args) -> {throw new TaskProcessor.TaskFailure("task fails");} ;
            }
        };

        Job job = processor.process();

        assertThat(this.retrievedTask.get(), is("task-id"));
        assertThat(this.statusChanges, contains(
                TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build(),
                TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.FAILURE).build()
        ));
        assertThat(this.result.get(), is(nullValue()));
        assertThat(this.logs, is(empty()));
        assertThat(job.status(), is(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build()));
    }

    @Test
    public void whenJobProcessingExceptio__thenStatusChangesToFAILURE_andJobHasFailureStatus() throws Exception {
        TaskJobProcessor<Person, Book> processor = new TaskJobProcessor<>(Job.builder()
                .arguments("task-id", "task-url")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                throw new JobProcessingException("failed creating task process");
            }
        };

        assertThrows(JobProcessingException.class, () -> processor.process());

        assertThat(this.statusChanges, is(empty()));
        assertThat(this.result.get(), is(nullValue()));
        assertThat(this.logs, is(empty()));
    }


    @Test
    public void givenTaskFragment__whenTaskSucceeds__thenTaskStatusIsStillRunning_andPartialResultSet_andJobHasSuccessStatus() throws Exception {
        TaskJobProcessor<Person, Book> processor = new TaskFragmentJobProcessor<Person, Book>(Job.builder()
                .arguments("task-id", "task-url")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                return (id, person, taskNotifier, args) -> Book.builder().author(person).build();
            }
        };

        Job job = processor.process();

        assertThat(this.retrievedTask.get(), is("task-id"));
        assertThat(this.statusChanges, contains(
                TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build()
        ));
        assertThat(this.result.get().toMap(), is(Book.builder().author(Person.builder().name("author name").build()).build().toMap()));
        assertThat(this.logs, is(empty()));
        assertThat(job.status(), is(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()));
    }


    @Test
    public void whenAdditionalArgumentsInJobCall__thenArgumentsPassedToTask() throws Exception {
        AtomicReference<String[]> taskArguments = new AtomicReference<>();
        TaskJobProcessor<Person, Book> processor = new TaskJobProcessor<>(Job.builder()
                .arguments("task-id", "task-url", "arg1", "arg2", "arg3")
                .build(), url -> this.taskClient, Person.class, Book.class) {
            @Override
            protected TaskProcessor<Person, Book> taskProcessor() throws JobProcessingException {
                return (id, person, taskNotifier, args) -> {
                    taskArguments.set(args);
                    return Book.builder().author(person).build();
                };
            }
        };

        processor.process();

        assertThat(taskArguments.get(), arrayContaining("arg1", "arg2", "arg3"));
    }
}