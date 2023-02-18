package org.codingmatters.tasks.support.handlers;

import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;

import java.util.function.Supplier;

public abstract class AbstractTaskHandler {
    private final Supplier<TaskEntryPointAdapter> adapterProvider;

    public AbstractTaskHandler(Supplier<TaskEntryPointAdapter> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    protected TaskEntryPointAdapter adapter() {
        return this.adapterProvider.get();
    }
}
