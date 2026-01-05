import { LoggingGrid } from "@/components/logging.grid";
import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingServerPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingGeneral[]>();

  useEffect(() => {
    turLoggingInstanceService.server().then(setLoggingInstances)
  }, [])

  return (
    <LoggingGrid gridItemList={loggingInstances || []} />
  )
}