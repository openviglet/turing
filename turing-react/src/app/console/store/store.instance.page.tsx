import { StoreInstanceForm } from "@/components/store/store.instance.form";
import type { TurStoreInstance } from "@/models/store/store-instance.model.ts";
import { TurStoreInstanceService } from "@/services/store/store.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turStoreInstanceService = new TurStoreInstanceService();

export default function StoreInstancePage() {
  const { id } = useParams() as { id: string };
  const [storeInstance, setStoreInstance] = useState<TurStoreInstance>({} as TurStoreInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turStoreInstanceService.get(id).then(setStoreInstance);
      setIsNew(false);
    }
  }, [id])
  return (
    <StoreInstanceForm value={storeInstance} isNew={isNew} />
  )
}
