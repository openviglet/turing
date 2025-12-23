import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import type { TurGridItem } from "@/models/ui/grid-item";
import { TurLLMInstanceService } from "@/services/llm/llm.service";
import { IconCpu2 } from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";

const turLLMInstanceService = new TurLLMInstanceService();

export default function LLMInstanceListPage() {
  const [llmInstances, setLlmInstances] = useState<TurLLMInstance[]>([]);

  useEffect(() => {
    turLLMInstanceService.query().then(setLlmInstances)
  }, [])
  const gridItemList: TurGridItem[] = useMemo(() => {
    return llmInstances
      ? llmInstances.map(({ id, title, description }) => ({
        id,
        name: title,
        description,
        url: ROUTES.LLM_INSTANCE + "/" + id
      }))
      : [];
  }, [llmInstances]);
  return (
    <>
      {llmInstances.length > 0 ? (
        <GridList gridItemList={gridItemList ?? []} />
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


