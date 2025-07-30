import { StoreInstanceForm } from "@/components/store.instance.form"
import { useParams } from "react-router-dom";
import type { TurStoreInstance } from "@/models/store-instance.model";
import { useEffect, useState } from "react";
import { TurStoreInstanceService } from "@/services/store.service";

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
