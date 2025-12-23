import { LoggingInstanceForm } from "@/components/logging.instance.form";
import type { TurLoggingInstance } from "@/models/logging/logging-instance.model.ts";
import { TurLoggingInstanceService } from "@/services/logging/logging.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turLoggingInstanceService = new TurLoggingInstanceService();

export default function LoggingInstancePage() {
  const { id } = useParams() as { id: string };
  const [loggingInstance, setLoggingInstance] = useState<TurLoggingInstance>({} as TurLoggingInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turLoggingInstanceService.get(id).then(setLoggingInstance);
      setIsNew(false);
    }
  }, [id])
  return (
    <LoggingInstanceForm value={loggingInstance} isNew={isNew} />
  )
}
