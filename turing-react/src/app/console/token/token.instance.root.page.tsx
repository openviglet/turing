import { SiteHeader } from "@/components/site-header"
import { IconCode } from "@tabler/icons-react";
import { Outlet } from "react-router-dom";

export default function TokenInstanceRootPage() {
  return (
    <>
      <SiteHeader turIcon={IconCode} title="API Token" urlBase="/admin/token/instance" urlNew="/admin/token/instance/new" />
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


