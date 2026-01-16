import { IndexingLoggingGrid } from "@/components/logging/indexing.logging.grid";
import type { TurLoggingIndexing } from "@/models/logging/logging-indexing.model";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingIndexingPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingIndexing[]>();

  useEffect(() => {
    turLoggingInstanceService.indexing().then(setLoggingInstances)
  }, [])

  return (
    <IndexingLoggingGrid gridItemList={loggingInstances || []} />
  )
}