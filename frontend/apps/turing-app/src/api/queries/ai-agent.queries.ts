import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { TurAIAgent } from '@/models/agent/ai-agent.model.ts';
import { TurAIAgentService } from '@/services/agent/ai-agent.service';
import { queryKeys } from './keys';

const service = new TurAIAgentService();

export function useAiAgents() {
  return useQuery<TurAIAgent[]>({
    queryKey: queryKeys.aiAgents.list(),
    queryFn: () => service.query(),
  });
}

export function useAiAgent(id: string | undefined) {
  return useQuery<TurAIAgent>({
    queryKey: id ? queryKeys.aiAgents.detail(id) : ['ai-agents', 'pending'],
    queryFn: () => service.get(id as string),
    enabled: Boolean(id) && id !== 'new',
  });
}

function invalidateAiAgents(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: queryKeys.aiAgents.all() });
}

export function useCreateAiAgent() {
  const queryClient = useQueryClient();
  return useMutation<TurAIAgent, Error, TurAIAgent>({
    mutationFn: (agent) => service.create(agent),
    onSuccess: () => invalidateAiAgents(queryClient),
  });
}

/**
 * Reused by the settings form and the four agent sub-forms (llm, mcp, custom-tool,
 * tools) that all save into the same agent record via partial updates.
 */
export function useUpdateAiAgent() {
  const queryClient = useQueryClient();
  return useMutation<TurAIAgent, Error, TurAIAgent>({
    // T1319 — the forms each hold a copy of the agent, so the version they carry
    // can predate this browser's own last save. Send the version the console last
    // received; the server refuses it only when someone else saved in between.
    mutationFn: (agent) => {
      const cached = agent.id
        ? queryClient.getQueryData<TurAIAgent>(queryKeys.aiAgents.detail(agent.id))
        : undefined;
      return service.update({ ...agent, version: cached?.version ?? agent.version });
    },
    onSuccess: (saved) => {
      if (saved?.id) {
        queryClient.setQueryData(queryKeys.aiAgents.detail(saved.id), saved);
      }
      invalidateAiAgents(queryClient);
    },
  });
}

export function useDeleteAiAgent() {
  const queryClient = useQueryClient();
  return useMutation<boolean, Error, TurAIAgent>({
    mutationFn: (agent) => service.delete(agent),
    onSuccess: (_ok, agent) => {
      if (agent?.id) {
        queryClient.removeQueries({ queryKey: queryKeys.aiAgents.detail(agent.id) });
      }
      invalidateAiAgents(queryClient);
    },
  });
}
