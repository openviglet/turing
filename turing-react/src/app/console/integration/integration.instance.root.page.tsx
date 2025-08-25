import { Page } from "@/components/page";
import { IconPlugConnectedX } from "@tabler/icons-react";

export default function IntegrationInstanceRootPage() {
  return (
    <Page turIcon={IconPlugConnectedX} title="Integration" urlBase="/admin/integration/instance" urlNew="/admin/integration/instance/new" />
  )
}