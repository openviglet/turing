import { IconZoomCode } from "@tabler/icons-react";
import { Page } from "@/components/page";
import { ROUTES } from "@/app/routes.const";

export default function SEInstanceRootPage() {
  return (
    <Page turIcon={IconZoomCode} title="Search Engine" urlBase={ROUTES.SE_INSTANCE} urlNew={`${ROUTES.SE_INSTANCE}/new`} />
  )
}
