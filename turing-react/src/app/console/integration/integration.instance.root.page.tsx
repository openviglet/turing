import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconPlugConnectedX } from "@tabler/icons-react";

export default function IntegrationInstanceRootPage() {
  return (
    <Page turIcon={IconPlugConnectedX} title="Integration" urlBase={ROUTES.INTEGRATION_INSTANCE} urlNew={`${ROUTES.INTEGRATION_INSTANCE}/new`} />
  )
}