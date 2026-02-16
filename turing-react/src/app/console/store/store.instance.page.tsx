import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { StoreInstanceForm } from "@/components/store/store.instance.form";
import type { TurStoreInstance } from "@/models/store/store-instance.model.ts";
import { TurStoreInstanceService } from "@/services/store/store.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turStoreInstanceService = new TurStoreInstanceService();

export default function StoreInstancePage() {
  const { id } = useParams() as { id: string };
  const [storeInstance, setStoreInstance] = useState<TurStoreInstance>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (id === "new") {
      turStoreInstanceService.query().then(() => setStoreInstance({} as TurStoreInstance)).catch(() => setError("Connection error or timeout while fetching Store service."));
    } else {
      turStoreInstanceService.get(id).then(setStoreInstance).catch(() => setError("Connection error or timeout while fetching Store instance."));
      setIsNew(false);
    }
  }, [id])
  return (
    <LoadProvider checkIsNotUndefined={storeInstance} error={error} tryAgainUrl={`${ROUTES.STORE_INSTANCE}/${id}`}>
      {storeInstance && <StoreInstanceForm value={storeInstance} isNew={isNew} />}
    </LoadProvider>
  )
}
