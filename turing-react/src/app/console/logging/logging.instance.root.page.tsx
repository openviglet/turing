import { Page } from "@/components/page";
import { IconReceiptRupee } from "@tabler/icons-react";

export default function LoggingInstanceRootPage() {
  return (
    <Page turIcon={IconReceiptRupee} title="Logging" urlBase="/admin/logging/instance" urlNew="/admin/logging/instance/new" />
  )
}


