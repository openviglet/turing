import { useParams } from "react-router-dom";
import type { TurLoggingInstance } from "@/models/logging/logging-instance.model.ts";
import { useEffect, useState } from "react";
import { TurLoggingInstanceService } from "@/services/logging.service";
import { LoggingInstanceForm } from "@/components/logging.instance.form";

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
