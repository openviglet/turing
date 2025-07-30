import { useParams } from "react-router-dom";
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model.ts";
import { useEffect, useState } from "react";
import { TurIntegrationInstanceService } from "@/services/integration.service";
import { IntegrationInstanceForm } from "@/components/integration.instance.form";

const turIntegrationInstanceService = new TurIntegrationInstanceService();

export default function IntegrationInstancePage() {
  const { id } = useParams() as { id: string };
  const [integrationInstance, setIntegrationInstance] = useState<TurIntegrationInstance>({} as TurIntegrationInstance);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turIntegrationInstanceService.get(id).then(setIntegrationInstance);
      setIsNew(false);
    }
  }, [id])
  return (
    <IntegrationInstanceForm value={integrationInstance} isNew={isNew} />
  )
}
