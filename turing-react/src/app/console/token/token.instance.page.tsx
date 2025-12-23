import { TokenInstanceForm } from "@/components/token.instance.form";
import type { TurTokenInstance } from "@/models/token/token-instance.model.ts";
import { TurTokenInstanceService } from "@/services/token/token.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

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
