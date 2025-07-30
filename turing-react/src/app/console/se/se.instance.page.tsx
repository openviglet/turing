import { SEInstanceForm } from "@/components/se.instance.form"
import { useParams } from "react-router-dom";
import type { TurSEInstance } from "@/models/se-instance.model";
import { useEffect, useState } from "react";
import { TurSEInstanceService } from "@/services/se.service";

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
