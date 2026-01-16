import { SEInstanceForm } from "@/components/se/se.instance.form";
import type { TurSEInstance } from "@/models/se/se-instance.model.ts";
import { TurSEInstanceService } from "@/services/se/se.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSEInstanceService = new TurSEInstanceService();

export default function SEInstancePage() {
  const { id } = useParams() as { id: string };
  const [seInstance, setSeInstance] = useState<TurSEInstance>({} as TurSEInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turSEInstanceService.get(id).then(setSeInstance);
      setIsNew(false);
    }
  }, [id])
  return (
    <SEInstanceForm value={seInstance} isNew={isNew} />
  )
}
