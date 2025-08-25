import { IconZoomCode } from "@tabler/icons-react";
import { Page } from "@/components/page";

export default function SEInstanceRootPage() {
  return (
    <Page turIcon={IconZoomCode} title="Search Engine" urlBase="/admin/se/instance" urlNew="/admin/se/instance/new" />
  )
}
