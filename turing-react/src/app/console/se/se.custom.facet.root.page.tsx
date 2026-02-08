import { IconFilter } from "@tabler/icons-react";
import { Page } from "@/components/page";
import { ROUTES } from "@/app/routes.const";

export default function SECustomFacetRootPage() {
  return (
    <Page turIcon={IconFilter} title="Custom Facets" urlBase={`${ROUTES.SE_INSTANCE}/custom-facet`} urlNew={`${ROUTES.SE_INSTANCE}/custom-facet/new`} />
  )
}
