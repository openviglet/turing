import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { CustomFacetParentGrid } from "@/components/sn/custom-facet/custom.facet.parent.grid";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteCustomFacetGroup } from "@/models/sn/sn-site-custom-facet-group.model";
import type { TurSNSiteCustomFacetParent } from "@/models/sn/sn-site-custom-facet-parent.model";
import { TurSNSiteCustomFacetGroupService } from "@/services/sn/sn.site.custom.facet.group.service";
import { TurSNSiteCustomFacetParentService } from "@/services/sn/sn.site.custom.facet.parent.service";
import { IconFilter } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const groupService = new TurSNSiteCustomFacetGroupService();
const parentService = new TurSNSiteCustomFacetParentService();

export default function SNSiteCustomFacetParentListPage() {
  const { id } = useParams() as { id: string };
  const [groups, setGroups] = useState<TurSNSiteCustomFacetGroup[]>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      parentService.query(),
      groupService.query()
    ]).then(([parents, grouped]) => {
      const counts = new Map<string, number>();
      grouped.forEach(g => counts.set(g.idName, g.count));
      const merged: TurSNSiteCustomFacetGroup[] = parents.map((p: TurSNSiteCustomFacetParent) => ({
        idName: p.idName,
        count: counts.get(p.idName) ?? 0
      }));
      setGroups(merged);
    }).catch(() => setError("Connection error or timeout while fetching custom facet groups."));
  }, [])

  return (
    <LoadProvider checkIsNotUndefined={groups} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      <>
        <SubPageHeader
          icon={IconFilter}
          feature="Custom Facets"
          name="Custom Facet Groups"
          description="Choose a group (NameID) to manage its validation items."
          urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/new/edit`}
        />
        <CustomFacetParentGrid items={groups} />
      </>
    </LoadProvider>
  )
}
