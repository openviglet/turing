import { SNCardList } from "@/components/sn.card.list";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import { TurSNSiteService } from "@/services/sn/sn.service";
import { useEffect, useState } from "react";

const turSNSiteService = new TurSNSiteService();

export default function SNSiteListPage() {
  const [snSite, setSnSite] = useState<TurSNSite[]>();

  useEffect(() => {
    turSNSiteService.query().then(setSnSite)
  }, [])
  return (
    <SNCardList items={snSite} />
  )
}


