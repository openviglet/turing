import type { TurIntegrationInstance } from "@/models/integration-instance.model";
import { TurIntegrationInstanceService } from "@/services/integration.service"
import { useEffect, useState } from "react";
import { IntegrationCardList } from "@/components/integration.card.list";

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


