import { Page } from "@/components/page";
import { IconDatabase } from "@tabler/icons-react";

export default function StoreInstanceListPage() {
  return (
    <Page turIcon={IconDatabase} title="Embedding Store" urlBase="/admin/store/instance" urlNew="/admin/store/instance/new" />
  )
}


