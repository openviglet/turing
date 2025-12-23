import { IntegrationCardList } from "@/components/integration.card.list";
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model.ts";
import { TurIntegrationInstanceService } from "@/services/integration/integration.service";
import { useEffect, useState } from "react";

const turIntegrationInstanceService = new TurIntegrationInstanceService();

export default function IntegrationInstanceListPage() {
  const [integrationInstances, setIntegrationInstances] = useState<TurIntegrationInstance[]>();

  useEffect(() => {
    turIntegrationInstanceService.query().then(setIntegrationInstances)
  }, [])
  return (
    <IntegrationCardList items={integrationInstances} />
  )
}


