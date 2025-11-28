import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconCode } from "@tabler/icons-react";

export default function TokenInstanceRootPage() {
  return (
    <Page turIcon={IconCode} title="API Token" urlBase={ROUTES.TOKEN_INSTANCE} urlNew={`${ROUTES.TOKEN_INSTANCE}/new`} />
  )
}


