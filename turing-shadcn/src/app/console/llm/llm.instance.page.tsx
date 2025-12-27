import { useParams } from "react-router-dom";
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import { useEffect, useState } from "react";
import { TurLLMInstanceService } from "@/services/llm.service";
import { LLMInstanceForm } from "@/components/llm.instance.form";

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
