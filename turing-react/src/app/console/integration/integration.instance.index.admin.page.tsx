import { IntegrationIndexAdminForm } from "@/components/integration/integration.index.admin.form";
import { SubPageHeader } from "@/components/sub.page.header";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  IconBolt,
  IconCloudDownload,
  IconCloudUpload,
  IconPlus,
  IconTools,
  IconTrash
} from "@tabler/icons-react";
import { useParams } from "react-router-dom";

export default function IntegrationInstanceIndexAdminPage() {
  const { id } = useParams() as { id: string };

  const items = [
    {
      value: "PUBLISHING",
      title: "Publishing",
      icon: IconCloudUpload,
      mode: "PUBLISHING" as const,
      color: "text-blue-500",
      activeBorder: "data-[state=active]:border-blue-500"
    },
    {
      value: "UNPUBLISHING",
      title: "Unpublishing",
      icon: IconCloudDownload,
      mode: "UNPUBLISHING" as const,
      color: "text-orange-500",
      activeBorder: "data-[state=active]:border-orange-500"
    },
    {
      value: "DEFAULT",
      title: "Default",
      icon: IconBolt,
      mode: "DEFAULT" as const,
      color: "text-gray-500",
      activeBorder: "data-[state=active]:border-gray-500"
    },
    {
      value: "CREATE",
      title: "Create",
      icon: IconPlus,
      mode: "CREATE" as const,
      color: "text-green-500",
      activeBorder: "data-[state=active]:border-green-500"
    },
    {
      value: "DELETE",
      title: "Delete",
      icon: IconTrash,
      mode: "DELETE" as const,
      color: "text-red-500",
      activeBorder: "data-[state=active]:border-red-500"
    }
  ];

  return (
    <>
      <SubPageHeader icon={IconTools} title="Index Admin" description="Submit indexing or deindexing requests for specific paths or URLs." />
      <div className="w-full mx-auto mt-6">
        <Tabs defaultValue="PUBLISHING" className="w-full">
          <TabsList className="w-full h-auto bg-transparent p-0 gap-4 flex-wrap justify-start">
            {items.map((item) => (
              <TabsTrigger
                key={item.value}
                value={item.value}
                className={`flex-1 min-w-[150px] flex flex-col items-center gap-3 py-6 h-auto border-2 border-transparent bg-card shadow-sm hover:bg-accent hover:text-accent-foreground data-[state=active]:shadow-md transition-all ${item.activeBorder}`}
              >
                <item.icon className={`w-8 h-8 ${item.color}`} />
                <span className="font-semibold text-base">{item.title}</span>
              </TabsTrigger>
            ))}
          </TabsList>
          
          <div className="mt-6 border rounded-lg p-6 bg-card shadow-sm">
            {items.map((item) => (
              <TabsContent key={item.value} value={item.value} className="mt-0 focus-visible:ring-0">
                 <div className="mb-6 pb-4 border-b">
                    <h3 className="text-xl font-semibold flex items-center gap-2">
                        <item.icon className={`w-6 h-6 ${item.color}`} />
                        {item.title} Operation
                    </h3>
                    <p className="text-muted-foreground text-sm mt-1">
                        Configure the parameters for the {item.title.toLowerCase()} operation below.
                    </p>
                 </div>
                <IntegrationIndexAdminForm integrationId={id} mode={item.mode} />
              </TabsContent>
            ))}
          </div>
        </Tabs>
      </div>
    </>
  )
}
