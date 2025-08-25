import { Page } from "@/components/page";
import { IconCpu2 } from "@tabler/icons-react";

export default function LLMInstanceRootPage() {
  return (
    <Page turIcon={IconCpu2} title="Language Model" urlBase="/admin/llm/instance" urlNew="/admin/llm/instance/new" />
  )
}


