import { ROUTES } from "@/app/routes.const";
import { AemMonitoringGrid } from "@/components/integration/aem.monitoring.grid";
import { SubPageHeader } from "@/components/sub.page.header";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { TurIntegrationMonitoring } from "@/models/integration/integration-monitoring.model";
import { TurIntegrationMonitoringService } from "@/services/integration/integration-monitoring.service";
import { IconGraph } from "@tabler/icons-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

const DEFAULT_SOURCE = "all";

const initialMonitoringState: TurIntegrationMonitoring = {
  sources: [],
  indexing: [],
};

export default function IntegrationInstanceMonitoringPage() {
  const navigate = useNavigate();
  const { id, source = DEFAULT_SOURCE } = useParams<{ id: string; source?: string }>();
  const [integrationMonitoring, setIntegrationMonitoring] = useState<TurIntegrationMonitoring>(initialMonitoringState);
  const [isLoading, setIsLoading] = useState(true);

  const monitoringService = useMemo(() => {
    return id ? new TurIntegrationMonitoringService(id) : null;
  }, [id]);

  useEffect(() => {
    if (!monitoringService || !id) return;

    const fetchData = async () => {
      setIsLoading(true);
      let redirected = false;
      try {
        if (source === DEFAULT_SOURCE) {
          const data = await monitoringService.query();
          if (data.sources && data.sources.length > 0) {
            navigate(`${ROUTES.INTEGRATION_INSTANCE}/${id}/monitoring/${data.sources[0]}`);
            redirected = true;
            return;
          }
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
        console.error("Failed to fetch monitoring data:", error);
        setIntegrationMonitoring(initialMonitoringState);
      } finally {
        if (!redirected) {
          setIsLoading(false);
        }
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
    <>
      <SubPageHeader
        icon={IconGraph}
        name="Monitoring"
        feature="Monitoring"
        description="Verify the current status of AEM content indexing."
      />
      <Tabs value={source} onValueChange={handleTabChange} className="mb-4 mt-2" >
        <TabsList>
          <TabsTrigger value={DEFAULT_SOURCE}>All</TabsTrigger>
          {integrationMonitoring.sources?.map((tab) => (
            <TabsTrigger key={tab} value={tab}>
              {tab}
            </TabsTrigger>
          )) || null}
        </TabsList>
      </Tabs>
      {isLoading ? (
        <div className="pr-4">
          <Skeleton className="h-50 w-full rounded-xl" />
          <Skeleton className="h-4 w-62.5" />
          <Skeleton className="h-4 w-50" />
        </div>
      ) : (
        <AemMonitoringGrid gridItemList={integrationMonitoring.indexing || []} />
      )}
    </>
  );
}
