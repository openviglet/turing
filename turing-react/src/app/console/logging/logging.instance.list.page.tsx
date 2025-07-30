import type { TurLoggingInstance } from "@/models/logging-instance.model";
import { TurLoggingInstanceService } from "@/services/logging.service"
import { useEffect, useState } from "react";
import { LoggingCardList } from "@/components/logging.card.list";

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