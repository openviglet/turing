import { SiteHeader } from "@/components/site-header"
import { IconCpu2 } from "@tabler/icons-react";
import { Outlet } from "react-router-dom";

export default function LLMInstanceRootPage() {
  return (
    <>
      <SiteHeader turIcon={IconCpu2} title="Language Model" urlBase="/console/llm/instance" urlNew="/console/llm/instance/new" />
      <div className="flex flex-1 flex-col">
        <div className="@container/main flex flex-1 flex-col gap-2">
          <div className="flex flex-col gap-4 py-4 md:gap-6 md:py-6">
            <Outlet />
          </div>
        </div>
      </div>
    </>
  )
}


