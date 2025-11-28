import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconSearch } from "@tabler/icons-react";

export default function SNSiteRootPage() {
  return (
    <Page turIcon={IconSearch} title="Semantic Navigation" urlBase={ROUTES.SN_INSTANCE} urlNew={`${ROUTES.SN_INSTANCE}/new`} />
  )
}


