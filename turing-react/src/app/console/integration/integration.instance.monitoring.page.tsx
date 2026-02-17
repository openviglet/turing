import { ROUTES } from "@/app/routes.const";
import { AemMonitoringGrid } from "@/components/integration/aem.monitoring.grid";
import { LoadProvider } from "@/components/loading-provider";
import { SubPageHeader } from "@/components/sub.page.header";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useBreadcrumb } from "@/contexts/breadcrumb.context";
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
  const { pushItem, popItem } = useBreadcrumb();
  useEffect(() => {
    let isMounted = true;
    let itemsAdded = 0;

    const fetchData = async () => {
      if (!monitoringService || !id) return;

      try {
        const data = source === DEFAULT_SOURCE
          ? await monitoringService.query()
          : await monitoringService.get(source);

        if (!isMounted) return;

        setIntegrationMonitoring({
          sources: data.sources || [],
          indexing: data.indexing || [],
        });

        pushItem({ label: "Monitoring", href: `${ROUTES.INTEGRATION_INSTANCE}/${id}/monitoring` });
        pushItem({ label: source === DEFAULT_SOURCE ? "All" : source });
        itemsAdded = 2;

      } catch (err) {
        console.error(err);
      }
    };

    fetchData();

    return () => {
      isMounted = false;
      for (let i = 0; i < itemsAdded; i++) {
        popItem();
      }
    };
  }, [source, id]);
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
