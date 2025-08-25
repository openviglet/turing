import { Page } from "@/components/page";
import { IconCode } from "@tabler/icons-react";

export default function TokenInstanceRootPage() {
  return (
    <Page turIcon={IconCode} title="API Token" urlBase="/admin/token/instance" urlNew="/admin/token/instance/new" />
  )
}


