import { AemMonitoringGrid } from "@/components/aem.monitoring.grid";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurIntegrationMonitoring } from "@/models/integration/integration-monitoring.model";
import { TurIntegrationMonitoringService } from "@/services/integration/integration-monitoring.service";
import { IconGraph } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";


export default function IntegrationInstanceMonitoringPage() {
  const { id } = useParams() as { id: string };
  const [integrationMonitoring, setIntegrationMonitoring] = useState<TurIntegrationMonitoring>({} as TurIntegrationMonitoring);
  useEffect(() => {
    const turIntegrationInstanceService = new TurIntegrationMonitoringService(id);
    turIntegrationInstanceService.query().then((data) => {
      setIntegrationMonitoring(data);
    });

  }, [id])
  return (
    <>
      <SubPageHeader icon={IconGraph} title="Monitoring" description="Verify the current status of AEM content indexing." />
      <AemMonitoringGrid gridItemList={integrationMonitoring.indexing || []} />
    </>
  )
}
