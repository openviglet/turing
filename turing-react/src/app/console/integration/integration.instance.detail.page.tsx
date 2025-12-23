import { IntegrationInstanceForm } from "@/components/integration.instance.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model";
import { TurIntegrationInstanceService } from "@/services/integration/integration.service";
import { IconSettings } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
const turIntegrationInstanceService = new TurIntegrationInstanceService();

export default function IntegrationInstanceDetailPage() {
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
    <>
      <SubPageHeader icon={IconSettings} title="Settings" description="Integration settings." />
      <IntegrationInstanceForm value={integrationInstance} isNew={isNew} />
    </>
  )
}
