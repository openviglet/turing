import { ROUTES } from "@/app/routes.const";
import { GridList } from "@/components/grid.list";
import type { TurSEInstance } from "@/models/se/se-instance.model.ts";
import type { TurGridItem } from "@/models/ui/grid-item";
import { TurSEInstanceService } from "@/services/se/se.service";
import { useEffect, useMemo, useState } from "react";

const turSEInstanceService = new TurSEInstanceService();

export default function SEInstanceListPage() {
  const [seInstances, setSeInstances] = useState<TurSEInstance[]>();

  useEffect(() => {
    turSEInstanceService.query().then(setSeInstances)

  }, [])
  const gridItemList: TurGridItem[] = useMemo(() => {
    return seInstances
      ? seInstances.map(({ id, title, description }) => ({
        id,
        name: title,
        description,
        url: ROUTES.SE_INSTANCE + "/" + id
      }))
      : [];
  }, [seInstances]);
  return (

    <GridList gridItemList={gridItemList ?? []} />

  )
}


