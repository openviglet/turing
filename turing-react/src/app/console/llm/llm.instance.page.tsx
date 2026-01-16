import { LLMInstanceForm } from "@/components/llm/llm.instance.form";
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import { TurLLMInstanceService } from "@/services/llm/llm.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turLLMInstanceService = new TurLLMInstanceService();

export default function LLMInstancePage() {
  const { id } = useParams() as { id: string };
  const [llmInstance, setLlmInstance] = useState<TurLLMInstance>({} as TurLLMInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turLLMInstanceService.get(id).then(setLlmInstance);
      setIsNew(false);
    }
  }, [id])
  return (
    <LLMInstanceForm value={llmInstance} isNew={isNew} />
  )
}
