import { SNSiteForm } from "@/components/sn.site.form"
import { useParams } from "react-router-dom";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { useEffect, useState } from "react";
import { TurSNSiteService } from "@/services/sn.service";
import { SubPageHeader } from "@/components/sub.page.header";
import { IconSettings } from "@tabler/icons-react";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteDetailPage() {
  const { id } = useParams() as { id: string };
  const [snSite, setSnSite] = useState<TurSNSite>({} as TurSNSite);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turSNSiteService.get(id).then(setSnSite);
      setIsNew(false);
    }
  }, [id])
  return (
    <>
      <SubPageHeader icon={IconSettings} title="Settings" description="Configure the Semantic Navigation Site." />
      <SNSiteForm value={snSite} isNew={isNew} />
    </>
  )
}
