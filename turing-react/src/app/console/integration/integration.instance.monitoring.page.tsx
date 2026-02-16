import { ROUTES } from "@/app/routes.const";
import { AemMonitoringGrid } from "@/components/integration/aem.monitoring.grid";
import { LoadProvider } from "@/components/loading-provider";
import { SubPageHeader } from "@/components/sub.page.header";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { TurIntegrationMonitoring } from "@/models/integration/integration-monitoring.model";
import { TurIntegrationMonitoringService } from "@/services/integration/integration-monitoring.service";
import { IconGraph } from "@tabler/icons-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

const DEFAULT_SOURCE = "all";

export default function IntegrationInstanceMonitoringPage() {
  const navigate = useNavigate();
  const { id, source = DEFAULT_SOURCE } = useParams<{ id: string; source?: string }>();
  const [integrationMonitoring, setIntegrationMonitoring] = useState<TurIntegrationMonitoring>();
  const [error, setError] = useState<string | null>(null);
  const monitoringService = useMemo(() => {
    return id ? new TurIntegrationMonitoringService(id) : null;
  }, [id]);

  useEffect(() => {
    if (!monitoringService || !id) return;
    const fetchData = async () => {
      try {
        if (source === DEFAULT_SOURCE) {
          const data = await monitoringService.query();
          setIntegrationMonitoring({
            sources: data.sources || [],
            indexing: data.indexing || [],
          });
        } else {
          const data = await monitoringService.get(source);
          setIntegrationMonitoring({
            sources: data.sources || [],
            indexing: data.indexing || [],
          });
        }
      } catch (error) {
        console.error("Error fetching monitoring data:", error);
        setError("Connection error or timeout while fetching monitoring data.");
      }
    };

    fetchData();
  }, [monitoringService, id, source]);

  const handleTabChange = useCallback((newSource: string) => {
    navigate(`${ROUTES.INTEGRATION_INSTANCE}/${id}/monitoring/${newSource}`);
  }, [navigate, id]);

  if (!id) {
    return <div>Invalid integration instance ID</div>;
  }

  return (
    <LoadProvider checkIsNotUndefined={integrationMonitoring} error={error} tryAgainUrl={`${ROUTES.INTEGRATION_INSTANCE}/${id}/monitoring`}>
      <SubPageHeader
        icon={IconGraph}
        name="Monitoring"
        feature="Monitoring"
        description="Verify the current status of AEM content indexing."
      />
      {integrationMonitoring && (
        <>
          <Tabs value={source} onValueChange={handleTabChange} className="mb-4 mt-2 px-6" >
            <TabsList>
              <TabsTrigger value={DEFAULT_SOURCE}>All</TabsTrigger>
              {integrationMonitoring.sources?.map((tab) => (
                <TabsTrigger key={tab} value={tab}>
                  {tab}
                </TabsTrigger>
              )) || null}
            </TabsList>
          </Tabs>
          <AemMonitoringGrid gridItemList={integrationMonitoring.indexing || []} />
        </>
      )}
    </LoadProvider>
  );
}
