import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurSEInstance } from "@/models/se/se-instance.model.ts";
import { TurSEInstanceService } from "@/services/se/se.service";
import { IconZoomCode } from "@tabler/icons-react";
import { useEffect, useState } from "react";

const turSEInstanceService = new TurSEInstanceService();

export default function SEInstanceListPage() {
  const [seInstances, setSeInstances] = useState<TurSEInstance[]>();

  useEffect(() => {
    turSEInstanceService.query().then(setSeInstances)

  }, [])
  const gridItemList = useGridAdapter(seInstances, {
    name: "title",
    description: "description",
    url: (item) => `${ROUTES.SE_INSTANCE}/${item.id}`
  });
  return (

    <>
      {gridItemList.length > 0 ? (
        <GridList gridItemList={gridItemList} />
      ) : (
        <BlankSlate
          icon={IconZoomCode}
          title="You don’t seem to have any search engine instance."
          description="Create a new instance to use in semantic navigation and chatbot."
          buttonText="New search engine instance"
          urlNew={`${ROUTES.SE_INSTANCE}/new`} />
      )}
    </>

  )
}


