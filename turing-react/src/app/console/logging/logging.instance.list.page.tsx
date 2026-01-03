import { GridLogging } from "@/components/grid.logging";
import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingInstanceListPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingGeneral[]>();

  useEffect(() => {
    turLoggingInstanceService.query().then(setLoggingInstances)
  }, [])

  return (
    <GridLogging gridItemList={loggingInstances || []} />
  )
}