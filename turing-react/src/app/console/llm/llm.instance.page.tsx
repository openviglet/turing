import { ROUTES } from "@/app/routes.const";
import { LLMInstanceForm } from "@/components/llm/llm.instance.form";
import { LoadProvider } from "@/components/loading-provider";
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import { TurLLMInstanceService } from "@/services/llm/llm.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turLLMInstanceService = new TurLLMInstanceService();

export default function LLMInstancePage() {
  const { id } = useParams() as { id: string };
  const [llmInstance, setLlmInstance] = useState<TurLLMInstance>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (id === "new") {
      turLLMInstanceService.query().then(() => setLlmInstance({} as TurLLMInstance)).catch(() => setError("Connection error or timeout while fetching LLM service."));
    } else {
      turLLMInstanceService.get(id).then(setLlmInstance).catch(() => setError("Connection error or timeout while fetching LLM instance."));
      setIsNew(false);
    }
  }, [id])
  return (
    <LoadProvider checkIsNotUndefined={llmInstance} error={error} tryAgainUrl={`${ROUTES.LLM_INSTANCE}/${id}`}>
      {llmInstance && <LLMInstanceForm value={llmInstance} isNew={isNew} />}
    </LoadProvider>
  )
}
