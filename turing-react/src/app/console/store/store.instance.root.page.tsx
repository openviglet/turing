import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconDatabase } from "@tabler/icons-react";

export default function StoreInstanceListPage() {
  return (
    <Page turIcon={IconDatabase} title="Embedding Store" urlBase={ROUTES.STORE_INSTANCE} urlNew={`${ROUTES.STORE_INSTANCE}/new`} />
  )
}


