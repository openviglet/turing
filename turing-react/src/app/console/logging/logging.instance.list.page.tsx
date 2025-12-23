import { ROUTES } from "@/app/routes.const";
import { GridList } from "@/components/grid.list";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurLoggingInstance } from "@/models/logging/logging-instance.model.ts";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingInstanceListPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingInstance[]>();

  useEffect(() => {
    turLoggingInstanceService.query().then(setLoggingInstances)
  }, [])
  const gridItemList = useGridAdapter(loggingInstances, {
    name: "title",
    description: "description",
    url: (item) => `${ROUTES.LOGGING_INSTANCE}/${item.id}`
  });
  return (
    <GridList gridItemList={gridItemList} />
  )
}