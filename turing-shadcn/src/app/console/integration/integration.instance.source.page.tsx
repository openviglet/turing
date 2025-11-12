import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { SubPageHeader } from "@/components/sub.page.header";
import { IconGitCommit } from "@tabler/icons-react";
import { IntegrationInstanceForm } from "@/components/integration.instance.form";
import type { TurIntegrationInstance } from "@/models/integration/integration-instance.model";
import { TurIntegrationInstanceService } from "@/services/integration.service";
const turIntegrationInstanceService = new TurIntegrationInstanceService();

export default function IntegrationInstanceSourcePage() {
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
      <SubPageHeader icon={IconGitCommit} title="Sources" description="Available AEM sources for indexing and configuration." />
      <IntegrationInstanceForm value={integrationInstance} isNew={isNew} />
    </>
  )
}
