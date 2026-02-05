import { SNSiteBehaviorForm } from "@/components/sn/sn.site.behavior.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { TurSNSiteService } from "@/services/sn/sn.service";
import { IconScale } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteBehaviorPage() {
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
      <SubPageHeader icon={IconScale} name="Behavior" feature="Behavior" description="How the search will behave during the search." />
      <SNSiteBehaviorForm value={snSite} isNew={isNew} />
    </>
  )
}
