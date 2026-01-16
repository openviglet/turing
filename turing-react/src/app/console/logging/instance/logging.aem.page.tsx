import { LoggingGrid } from "@/components/logging/logging.grid";
import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingAemPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingGeneral[]>();

  useEffect(() => {
    turLoggingInstanceService.aem().then(setLoggingInstances)
  }, [])

  return (
    <LoggingGrid gridItemList={loggingInstances || []} />
  )
}