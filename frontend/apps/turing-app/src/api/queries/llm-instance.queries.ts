import { useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type {
  TurLLMInstance,
  TurLlmCatalogChangeNotification,
  TurLlmDeprecatedModelUsage,
} from '@/models/llm/llm-instance.model.ts';
import { TurLLMInstanceService } from '@/services/llm/llm.service';
import { queryKeys } from './keys';

const service = new TurLLMInstanceService();

export function useLlmInstances() {
  return useQuery<TurLLMInstance[]>({
    queryKey: queryKeys.llmInstances.list(),
    queryFn: () => service.query(),
  });
}

/**
 * T1315 — the instances an agent names by id, read from the cached list (an
 * agent holds `llmInstanceIds`, not the instances themselves).
 */
export function useAgentLlmInstances(llmInstanceIds: readonly string[] | undefined) {
  const { data } = useLlmInstances();
  return useMemo(() => {
    const ids = new Set(llmInstanceIds ?? []);
    return (data ?? []).filter((instance) => ids.has(instance.id));
  }, [data, llmInstanceIds]);
}

/** T782 — instances configured with a catalog-deprecated/retired model. */
export function useLlmDeprecations() {
  return useQuery<TurLlmDeprecatedModelUsage[]>({
    queryKey: queryKeys.llmInstances.deprecations(),
    queryFn: () => service.deprecations(),
  });
}

/** T788 — catalog change-feed notifications for in-use models. */
export function useLlmCatalogChanges() {
  return useQuery<TurLlmCatalogChangeNotification[]>({
    queryKey: queryKeys.llmInstances.changeFeed(),
    queryFn: () => service.changeFeed(),
  });
}

export function useLlmInstance(id: string | undefined) {
  return useQuery<TurLLMInstance>({
    queryKey: id ? queryKeys.llmInstances.detail(id) : ['llm-instances', 'pending'],
    queryFn: () => service.get(id as string),
    enabled: Boolean(id) && id !== 'new',
  });
}

function invalidateLlmInstances(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: queryKeys.llmInstances.all() });
}

export function useCreateLlmInstance() {
  const queryClient = useQueryClient();
  return useMutation<TurLLMInstance, Error, TurLLMInstance>({
    mutationFn: (instance) => service.create(instance),
    onSuccess: () => invalidateLlmInstances(queryClient),
  });
}

export function useUpdateLlmInstance() {
  const queryClient = useQueryClient();
  return useMutation<TurLLMInstance, Error, TurLLMInstance>({
    // T1319 — send the version this console last received, so a form holding an
    // older copy does not refuse its own follow-up save.
    mutationFn: (instance) => {
      const cached = instance.id
        ? queryClient.getQueryData<TurLLMInstance>(queryKeys.llmInstances.detail(instance.id))
        : undefined;
      return service.update({ ...instance, version: cached?.version ?? instance.version });
    },
    onSuccess: (saved) => {
      if (saved?.id) {
        queryClient.setQueryData(queryKeys.llmInstances.detail(saved.id), saved);
      }
      invalidateLlmInstances(queryClient);
    },
  });
}

export function useDeleteLlmInstance() {
  const queryClient = useQueryClient();
  return useMutation<boolean, Error, TurLLMInstance>({
    mutationFn: (instance) => service.delete(instance),
    onSuccess: (_ok, instance) => {
      if (instance?.id) {
        queryClient.removeQueries({ queryKey: queryKeys.llmInstances.detail(instance.id) });
      }
      invalidateLlmInstances(queryClient);
    },
  });
}
