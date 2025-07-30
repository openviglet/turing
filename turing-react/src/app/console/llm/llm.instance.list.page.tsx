import type { TurLLMInstance } from "@/models/llm-instance.model";
import { TurLLMInstanceService } from "@/services/llm.service"
import { useEffect, useState } from "react";
import { LLMCardList } from "@/components/llm.card.list";

const turLLMInstanceService = new TurLLMInstanceService();

export default function LLMInstanceListPage() {
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>();

  useEffect(() => {
    turLLMInstanceService.query().then(setLlmInstances)
  }, [])
  return (
    <LLMCardList items={llmInstances} />
  )
}


