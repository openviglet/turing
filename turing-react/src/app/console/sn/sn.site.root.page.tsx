import { Page } from "@/components/page";
import { IconSearch } from "@tabler/icons-react";

export default function SNSiteRootPage() {
  return (
    <Page turIcon={IconSearch} title="Semantic Navigation" urlBase="/admin/sn/instance" urlNew="/admin/sn/instance/new" />
  )
}


