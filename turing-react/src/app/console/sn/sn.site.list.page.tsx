import { TurSNSiteService } from "@/services/sn.service"
import { useEffect, useState } from "react";
import { SNCardList } from "@/components/sn.card.list";
import type { TurSNSite } from "@/models/sn-site.model";

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


