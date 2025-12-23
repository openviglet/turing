import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurTokenInstance } from "@/models/token/token-instance.model.ts";
import { TurTokenInstanceService } from "@/services/token/token.service";
import { IconCode } from "@tabler/icons-react";
import { useEffect, useState } from "react";

const turTokenInstanceService = new TurTokenInstanceService();

export default function TokenInstanceListPage() {
  const [tokenInstances, setTokenInstances] = useState<TurTokenInstance[]>();

  useEffect(() => {
    turTokenInstanceService.query().then(setTokenInstances)
  }, [])
  const gridItemList = useGridAdapter(tokenInstances, {
    name: "title",
    description: "description",
    url: (item) => `${ROUTES.TOKEN_INSTANCE}/${item.id}`
  });
  return (
    <>
      {gridItemList.length > 0 ? (
        <GridList gridItemList={gridItemList} />
      ) : (
        <BlankSlate
          icon={IconCode}
          title="You don’t seem to have any token instance."
          description="Create a new token to allow access to the API."
          buttonText="New token instance"
          urlNew={`${ROUTES.TOKEN_INSTANCE}/new`} />
      )}
    </>
  )
}


