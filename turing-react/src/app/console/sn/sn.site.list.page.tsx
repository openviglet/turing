import { ROUTES } from "@/app/routes.const";
import { GridList } from "@/components/grid.list";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import type { TurGridItem } from "@/models/ui/grid-item";
import { TurSNSiteService } from "@/services/sn/sn.service";
import { useEffect, useMemo, useState } from "react";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteListPage() {
  const [snInstances, setSnInstances] = useState<TurSNSite[]>();

  useEffect(() => {
    turSNSiteService.query().then(setSnInstances)
  }, [])
  const gridItemList: TurGridItem[] = useMemo(() => {
    return snInstances
      ? snInstances.map(({ id, name, description }) => ({
        id,
        name,
        description,
        url: ROUTES.SN_INSTANCE + "/" + id
      }))
      : [];
  }, [snInstances]);
  return (
    <GridList gridItemList={gridItemList ?? []} />
  )
}


