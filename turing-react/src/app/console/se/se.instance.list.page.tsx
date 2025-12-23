import { SECardList } from "@/components/se.card.list";
import type { TurSEInstance } from "@/models/se/se-instance.model.ts";
import { TurSEInstanceService } from "@/services/se/se.service";
import { useEffect, useState } from "react";

const turSEInstanceService = new TurSEInstanceService();

export default function SEInstanceListPage() {
  const [seInstances, setSeInstances] = useState<TurSEInstance[]>();

  useEffect(() => {
    turSEInstanceService.query().then(setSeInstances)
  }, [])
  return (
    <SECardList items={seInstances} />

  )
}


