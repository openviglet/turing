import { LoggingCardList } from "@/components/logging.card.list";
import type { TurLoggingInstance } from "@/models/logging/logging-instance.model.ts";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingInstanceListPage() {
  const [loggingInstances, setLoggingInstances] = useState<TurLoggingInstance[]>();

  useEffect(() => {
    turLoggingInstanceService.query().then(setLoggingInstances)
  }, [])
  return (
    <LoggingCardList items={loggingInstances} />
  )
}