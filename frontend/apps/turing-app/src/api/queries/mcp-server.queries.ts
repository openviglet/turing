import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { TurMcpServer } from '@/models/mcp/mcp-server.model.ts';
import { TurMcpServerService } from '@/services/mcp/mcp-server.service';
import { queryKeys } from './keys';

const service = new TurMcpServerService();

export function useMcpServers() {
  return useQuery<TurMcpServer[]>({
    queryKey: queryKeys.mcpServers.list(),
    queryFn: () => service.query(),
  });
}

export function useMcpServer(id: string | undefined) {
  return useQuery<TurMcpServer>({
    queryKey: id ? queryKeys.mcpServers.detail(id) : ['mcp-servers', 'detail', 'pending'],
    queryFn: () => service.get(id as string),
    enabled: Boolean(id),
  });
}

function invalidateMcpServers(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: queryKeys.mcpServers.all() });
}

export function useCreateMcpServer() {
  const queryClient = useQueryClient();
  return useMutation<TurMcpServer, Error, TurMcpServer>({
    mutationFn: (server) => service.create(server),
    onSuccess: () => invalidateMcpServers(queryClient),
  });
}

export function useUpdateMcpServer() {
  const queryClient = useQueryClient();
  return useMutation<TurMcpServer, Error, TurMcpServer>({
    // T1319 — send the version this console last received, so a form holding an
    // older copy does not refuse its own follow-up save.
    mutationFn: (server) => {
      const cached = server.id
        ? queryClient.getQueryData<TurMcpServer>(queryKeys.mcpServers.detail(server.id))
        : undefined;
      return service.update({ ...server, version: cached?.version ?? server.version });
    },
    onSuccess: (saved) => {
      if (saved?.id) {
        queryClient.setQueryData(queryKeys.mcpServers.detail(saved.id), saved);
      }
      invalidateMcpServers(queryClient);
    },
  });
}

export function useDeleteMcpServer() {
  const queryClient = useQueryClient();
  return useMutation<boolean, Error, TurMcpServer>({
    mutationFn: (server) => service.delete(server),
    onSuccess: (_ok, server) => {
      if (server?.id) {
        queryClient.removeQueries({ queryKey: queryKeys.mcpServers.detail(server.id) });
      }
      invalidateMcpServers(queryClient);
    },
  });
}
