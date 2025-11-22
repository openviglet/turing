import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { StoreCardList } from "@/components/store.card.list";
import type { TurStoreInstance } from "@/models/store/store-instance.model.ts";
import { TurStoreInstanceService } from "@/services/store.service";
import { IconDatabase } from "@tabler/icons-react";
import { useEffect, useState } from "react";

const turStoreInstanceService = new TurStoreInstanceService();

export default function StoreInstanceListPage() {
  const [storeInstances, setStoreInstances] = useState<TurStoreInstance[]>([]);

  useEffect(() => {
    turStoreInstanceService.query().then(setStoreInstances)
  }, [])
  return (
    <>
      {storeInstances.length > 0 ? (
        <StoreCardList items={storeInstances} />
      ) : (
        <BlankSlate
          icon={IconDatabase}
          title="You don’t seem to have any embedding store instance."
          description="Create a new instance and use it in semantic navigation and chatbot."
          buttonText="New embedding store instance"
          urlNew={`${ROUTES.STORE_INSTANCE}/new`} />
      )}
    </>
  )
}


