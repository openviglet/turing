import { SiteHeader } from "@/components/site-header"
import { Outlet } from "react-router-dom";
import { IconZoomCode } from "@tabler/icons-react";

export default function SEInstanceRootPage() {
  return (
    <>
      <SiteHeader turIcon={IconZoomCode} title="Search Engine" urlBase="/admin/se/instance"  urlNew="/admin/se/instance/new"/>
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
