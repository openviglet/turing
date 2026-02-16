import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { LoggingGrid } from "@/components/logging/logging.grid";
import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingServerPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingGeneral[]>();
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    turLoggingInstanceService.server().then(setLoggingInstances).catch(() => setError("Connection error or timeout while fetching server logging."));
  }, [])

  return (
    <LoadProvider checkIsNotUndefined={loggingInstances} error={error} tryAgainUrl={`${ROUTES.LOGGING_INSTANCE}/server`}>
      <LoggingGrid gridItemList={loggingInstances || []} />
    </LoadProvider>
  )
}