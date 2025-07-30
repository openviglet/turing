import type { TurTokenInstance } from "@/models/token/token-instance.model.ts";
import { TurTokenInstanceService } from "@/services/token.service"
import { useEffect, useState } from "react";
import { TokenCardList } from "@/components/token.card.list";

const turTokenInstanceService = new TurTokenInstanceService();

export default function TokenInstanceListPage() {
  const [tokenInstances, setTokenInstances] = useState<TurTokenInstance[]>();

  useEffect(() => {
    turTokenInstanceService.query().then(setTokenInstances)
  }, [])
  return (
    <TokenCardList items={tokenInstances} />
  )
}


