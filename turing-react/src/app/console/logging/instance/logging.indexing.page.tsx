import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { IndexingLoggingGrid } from "@/components/logging/indexing.logging.grid";
import type { TurLoggingIndexing } from "@/models/logging/logging-indexing.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingIndexingPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingIndexing[]>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    turLoggingInstanceService.indexing().then(setLoggingInstances).catch(() => setError("Connection error or timeout while fetching indexing logging."));
  }, [])

  return (
    <LoadProvider checkIsNotUndefined={loggingInstances} error={error} tryAgainUrl={`${ROUTES.LOGGING_INSTANCE}/indexing`}>
      <IndexingLoggingGrid gridItemList={loggingInstances || []} />
    </LoadProvider>

  )
}