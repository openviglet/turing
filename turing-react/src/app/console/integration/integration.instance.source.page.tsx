import { ROUTES } from "@/app/routes.const";
import { IntegrationSourceForm } from "@/components/integration/integration.source.form";
import { SubPageHeader } from "@/components/sub.page.header";
import { useAemSourceService } from "@/contexts/TuringServiceContext";
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model";
import { IconGitCommit } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";


export default function IntegrationInstanceSourcePage() {
  const navigate = useNavigate();
  const { id, sourceId } = useParams() as { id: string, sourceId: string };
  const [integrationAemSource, setIntegrationAemSource] = useState<TurIntegrationAemSource>({} as TurIntegrationAemSource);
  const [isNew, setIsNew] = useState<boolean>(true);
  const turIntegrationAemSourceService = useAemSourceService(id);
  const [open, setOpen] = useState(false);
  useEffect(() => {
    if (id !== "new") {
      turIntegrationAemSourceService.get(sourceId).then(setIntegrationAemSource);
      setIsNew(false);
    }
  }, [id, sourceId, turIntegrationAemSourceService]);
  async function onDelete() {
    try {
      if (await turIntegrationAemSourceService.delete(integrationAemSource)) {
        toast.success(`The ${integrationAemSource.name} AEM Source Instance was deleted`);
        navigate(`${ROUTES.INTEGRATION_INSTANCE}/${id}/source`);
      } else {
        toast.error(`The ${integrationAemSource.name} AEM Source Instance was not deleted`);
      }

    } catch (error) {
      console.error("Form submission error", error);
      toast.error(`The ${integrationAemSource.name} AEM Source Instance was not deleted`);
    }
    setOpen(false);
  }
  return (
    <>
      <SubPageHeader icon={IconGitCommit} name="Sources"
        feature="Sources"
        description="Available AEM sources for indexing and configuration."
        onDelete={onDelete}
        open={open}
        setOpen={setOpen} />
      <IntegrationSourceForm value={integrationAemSource} isNew={isNew} integrationId={id} />
    </>
  )
}
