import { Page } from "@/components/page"
import { ROUTES } from "../routes.const"

export default function ConsolePage() {
  return (
    <Page title="Administration Console" urlBase={ROUTES.CONSOLE} />
  )
}
