import { TokenInstanceForm } from "@/components/token.instance.form"
import { useParams } from "react-router-dom";
import type { TurTokenInstance } from "@/models/token-instance.model";
import { useEffect, useState } from "react";
import { TurTokenInstanceService } from "@/services/token.service";

const turTokenInstanceService = new TurTokenInstanceService();

export default function TokenInstancePage() {
  const { id } = useParams() as { id: string };
  const [tokenInstance, setTokenInstance] = useState<TurTokenInstance>({} as TurTokenInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turTokenInstanceService.get(id).then(setTokenInstance);
      setIsNew(false);
    }
  }, [id])
  return (
   <TokenInstanceForm value={tokenInstance} isNew={isNew} />
  )
}
