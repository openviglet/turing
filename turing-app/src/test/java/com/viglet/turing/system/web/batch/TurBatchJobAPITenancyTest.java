package com.viglet.turing.system.web.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.viglet.turing.batch.persistence.model.TurBatchJob;
import com.viglet.turing.batch.persistence.repository.TurBatchJobRepository;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.tenant.TurInfraTenantScope;

/**
 * T1335 — a batch job belongs to whoever can see its LLM instance.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurBatchJobAPITenancyTest {

    private final TurBatchJobRepository jobs = mock(TurBatchJobRepository.class);
    private final TurLLMInstanceRepository llms = mock(TurLLMInstanceRepository.class);
    private final TurInfraTenantScope scope = mock(TurInfraTenantScope.class);
    private final TurBatchJobAPI api = new TurBatchJobAPI(jobs, new TurConfigProperties(), llms, scope);

    private static TurBatchJob job(String id, String instanceId) {
        TurBatchJob job = new TurBatchJob();
        job.setId(id);
        job.setInstanceId(instanceId);
        return job;
    }

    @SuppressWarnings("unchecked")
    private void tenancyOn(String tenant) {
        when(scope.visibleList(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Function<String, List<?>>) inv.getArgument(1)).apply(tenant));
    }

    @Test
    void withTenancyOnTheListAndCountCoverOnlyVisibleInstances() {
        tenancyOn("t1");
        TurLLMInstance mine = new TurLLMInstance();
        mine.setId("llm-mine");
        when(llms.findVisibleToTenant("t1")).thenReturn(List.of(mine));
        when(jobs.findByInstanceIdInOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(List.of(job("j1", "llm-mine")));
        when(jobs.countByResultsCollectedFalseAndInstanceIdIn(List.of("llm-mine"))).thenReturn(1L);

        assertThat(api.jobs(50)).extracting(TurBatchJobAPI.BatchJobView::id).containsExactly("j1");
        assertThat(api.status().openJobs()).isEqualTo(1L);
        verify(jobs, never()).findByOrderByCreatedAtDesc(any());
        verify(jobs, never()).countByResultsCollectedFalse();
    }

    @Test
    void aTenantThatSeesNoInstanceSeesNoJob() {
        tenancyOn("t2");
        when(llms.findVisibleToTenant("t2")).thenReturn(List.of());

        assertThat(api.jobs(50)).isEmpty();
        assertThat(api.status().openJobs()).isZero();
    }

    @SuppressWarnings("unchecked")
    @Test
    void withTenancyOffEveryJobIsListed() {
        when(scope.visibleList(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<List<?>>) inv.getArgument(0)).get());
        when(jobs.findByOrderByCreatedAtDesc(any())).thenReturn(List.of(job("j1", "a"), job("j2", "b")));

        assertThat(api.jobs(50)).hasSize(2);
    }
}
