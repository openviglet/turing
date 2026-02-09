import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconReceiptRupee } from "@tabler/icons-react";

export default function LoggingInstanceRootPage() {
  return (
    <Page turIcon={IconReceiptRupee} title="Logging" urlBase={ROUTES.LOGGING_INSTANCE} />
  )
}


