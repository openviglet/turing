import { IntegrationSourceForm } from "@/components/integration/integration.source.form";
import { SubPageHeader } from "@/components/sub.page.header";
import { useAemSourceService } from "@/contexts/TuringServiceContext";
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model";
import { IconGitCommit } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";


export default function IntegrationInstanceSourcePage() {
  const { id, sourceId } = useParams() as { id: string, sourceId: string };
  const [integrationAemSource, setIntegrationAemSource] = useState<TurIntegrationAemSource>({} as TurIntegrationAemSource);
  const [isNew, setIsNew] = useState<boolean>(true);
  const turIntegrationAemSourceService = useAemSourceService(id);

  useEffect(() => {
    if (id !== "new") {
      turIntegrationAemSourceService.get(sourceId).then(setIntegrationAemSource);
      setIsNew(false);
    }
  }, [id, sourceId, turIntegrationAemSourceService]);

  return (
    <>
      <SubPageHeader icon={IconGitCommit} name="Sources" feature="Sources" description="Available AEM sources for indexing and configuration." />
      <IntegrationSourceForm value={integrationAemSource} isNew={isNew} integrationId={id} />
    </>
  )
}
