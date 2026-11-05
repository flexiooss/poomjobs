package org.codingmatters.tasks.support.specs;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registre de jobs (poomjobs) remplacé par son client de test généré : enregistre les jobs soumis et les accepte,
 * les refuse, ou se montre injoignable sur demande d'un scénario.
 */
public final class JobRegistryDouble {
    /** Adresse où rien n'écoute : un appel y échoue en erreur d'entrée/sortie. */
    private static final String UNREACHABLE_URL = "http://localhost:1";

    private enum Mode { ACCEPTING, REFUSING, UNREACHABLE }

    private final List<JobCollectionPostRequest> submitted = new CopyOnWriteArrayList<>();
    private final PoomjobsJobRegistryAPIClient reachable = new PoomjobsJobRegistryAPIHandlersClient(
            new PoomjobsJobRegistryAPIHandlers.Builder().jobCollectionPostHandler(this::submit).build());
    private final PoomjobsJobRegistryAPIClient unreachable = new PoomjobsJobRegistryAPIRequesterClient(
            new OkHttpRequesterFactory(OkHttpClientWrapper.build(), () -> UNREACHABLE_URL), new JsonFactory(), UNREACHABLE_URL);
    private volatile Mode mode = Mode.ACCEPTING;

    /** Client du registre, dans le mode courant. */
    public PoomjobsJobRegistryAPIClient client() {
        return this.mode == Mode.UNREACHABLE ? this.unreachable : this.reachable;
    }

    public List<JobCollectionPostRequest> submitted() {
        return List.copyOf(this.submitted);
    }

    public void refuseJobs() {
        this.mode = Mode.REFUSING;
    }

    public void becomeUnreachable() {
        this.mode = Mode.UNREACHABLE;
    }

    void reset() {
        this.submitted.clear();
        this.mode = Mode.ACCEPTING;
    }

    private JobCollectionPostResponse submit(JobCollectionPostRequest request) {
        this.submitted.add(request);
        if (this.mode == Mode.ACCEPTING) {
            return JobCollectionPostResponse.builder()
                    .status201(st -> st.xEntityId("job-" + this.submitted.size()))
                    .build();
        }
        return JobCollectionPostResponse.builder()
                .status500(st -> st.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                .build();
    }
}
