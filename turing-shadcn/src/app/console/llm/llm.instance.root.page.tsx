import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconCpu2 } from "@tabler/icons-react";

export default function LLMInstanceRootPage() {
  return (
    <Page turIcon={IconCpu2} title="Language Model" urlBase={ROUTES.LLM_INSTANCE} urlNew={`${ROUTES.LLM_INSTANCE}/new`} />
  )
}


