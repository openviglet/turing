import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { LLMCardList } from "@/components/llm.card.list";
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import { TurLLMInstanceService } from "@/services/llm.service";
import { IconCpu2 } from "@tabler/icons-react";
import { useEffect, useState } from "react";

const turLLMInstanceService = new TurLLMInstanceService();

export default function LLMInstanceListPage() {
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([]);

  useEffect(() => {
    turLLMInstanceService.query().then(setLlmInstances)
  }, [])
  return (
    <>
      {llmInstances.length > 0 ? (
        <LLMCardList items={llmInstances} />
      ) : (
        <BlankSlate
          icon={IconCpu2}
          title="You don’t seem to have any language model instance."
          description="Create a new instance and use it in semantic navigation and chatbot."
          buttonText="New language model instance"
          urlNew={`${ROUTES.LLM_INSTANCE}/new`} />
      )}
    </>
  )
}


