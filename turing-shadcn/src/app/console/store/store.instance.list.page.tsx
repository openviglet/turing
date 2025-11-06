import type { TurStoreInstance } from "@/models/store/store-instance.model.ts";
import { TurStoreInstanceService } from "@/services/store.service"
import { useEffect, useState } from "react";
import { StoreCardList } from "@/components/store.card.list";

const turStoreInstanceService = new TurStoreInstanceService();

export default function StoreInstanceListPage() {
  const [storeInstances, setStoreInstances] = useState<TurStoreInstance[]>();

  useEffect(() => {
    turStoreInstanceService.query().then(setStoreInstances)
  }, [])
  return (
    <StoreCardList items={storeInstances} />
  )
}


