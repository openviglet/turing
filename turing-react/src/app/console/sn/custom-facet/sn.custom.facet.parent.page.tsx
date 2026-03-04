import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { SNSiteCustomFacetParentForm } from "@/components/sn/custom-facet/sn.custom.facet.parent.form";
import type { TurSNSiteCustomFacetParent } from "@/models/sn/sn-site-custom-facet-parent.model";
import { TurSNSiteCustomFacetParentService } from "@/services/sn/sn.site.custom.facet.parent.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const service = new TurSNSiteCustomFacetParentService();

export default function SNSiteCustomFacetParentPage() {
  const { id, groupIdName } = useParams() as { id: string, groupIdName: string };
  const [group, setGroup] = useState<TurSNSiteCustomFacetParent>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (groupIdName === "new") {
      setGroup({ idName: "", attribute: "", selection: "" } as TurSNSiteCustomFacetParent);
    } else {
      service.get(groupIdName).then(setGroup).catch(() => setError("Connection error or timeout while fetching custom facet group."));
      setIsNew(false);
    }
  }, [groupIdName])
  return (
    <LoadProvider checkIsNotUndefined={group} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      {group && <SNSiteCustomFacetParentForm value={group} isNew={isNew} />}
    </LoadProvider>
  )
}
