import { IntegrationIndexAdminForm } from "@/components/integration/integration.index.admin.form";
import { SubPageHeader } from "@/components/sub.page.header";
import { IconTools } from "@tabler/icons-react";
import { useParams } from "react-router-dom";

export default function IntegrationInstanceIndexAdminPage() {
  const { id } = useParams() as { id: string };
  return (
    <>
      <SubPageHeader icon={IconTools} title="Index Admin" description="Submit indexing or deindexing requests for specific paths or URLs." />
      <IntegrationIndexAdminForm integrationId={id} />
    </>
  )
}
