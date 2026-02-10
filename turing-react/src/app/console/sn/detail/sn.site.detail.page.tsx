import { SNSiteForm } from "@/components/sn/sn.site.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { TurSNSiteService } from "@/services/sn/sn.service";
import { IconSettings } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

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
      <SubPageHeader icon={IconSettings} name="Settings" feature="Settings" description="Configure the Semantic Navigation Site." />
      <SNSiteForm value={snSite} isNew={isNew} />
    </>
  )
}
