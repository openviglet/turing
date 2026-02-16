import { ROUTES } from "@/app/routes.const";
import { IntegrationIndexingManagerForm } from "@/components/integration/integration.index.manager.form";
import { LoadProvider } from "@/components/loading-provider";
import { SubPageHeader } from "@/components/sub.page.header";
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model";
import { TurIntegrationAemSourceService } from "@/services/integration/integration-aem-source.service";
import {
  IconAdjustmentsSearch,
  IconCloudDownload,
  IconCloudUpload,
  IconInfoCircle,
  IconPlus,
  IconTrash
} from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";

export default function IntegrationInstanceIndexAdminPage() {
  const { id } = useParams() as { id: string };
  const [error, setError] = useState<string | null>(null);
  const turIntegrationAemSourceService = useMemo(() => new TurIntegrationAemSourceService(id), [id]);
  const [sources, setSources] = useState<TurIntegrationAemSource[]>()
  useEffect(() => {
    turIntegrationAemSourceService.query().then(setSources).catch(() => setError("Failed to load integration details"));
  }, [turIntegrationAemSourceService])

  const items = [
    {
      value: "INDEXING",
      title: "Indexing",
      icon: IconPlus,
      mode: "INDEXING" as const,
      color: "text-green-500",
      activeBorder: "data-[state=active]:border-green-500",
      description: "This action affects both environments, but content will only appear in Publish if it is published in AEM.",
      detailedDescription: "Detailed breakdown of the indexing process including recursive crawling strategies, content extraction pipelines, and metadata enrichment phases. This ensures all new content is properly ingested into the search index."
    },
    {
      value: "DEINDEXING",
      title: "Deindexing",
      icon: IconTrash,
      mode: "DEINDEXING" as const,
      color: "text-red-500",
      activeBorder: "data-[state=active]:border-red-500",
      description: "This action will deindex content from both Author and Publish environments.",
      detailedDescription: "Comprehensive removal procedure that safely detaches content from the search index, cleans up associated vector embeddings, and updates the index stats. Use this to remove obsolete or sensitive content."
    },
    {
      value: "PUBLISHING",
      title: "Publishing",
      icon: IconCloudUpload,
      mode: "PUBLISHING" as const,
      color: "text-blue-500",
      activeBorder: "data-[state=active]:border-blue-500",
      description: "This action will only affect the Publish environment.",
      detailedDescription: "Synchronization workflow that propagates content changes from the Author environment to the live Publish environment. This includes cache invalidation and CDN updates to ensure users see the latest version."
    },
    {
      value: "UNPUBLISHING",
      title: "Unpublishing",
      icon: IconCloudDownload,
      mode: "UNPUBLISHING" as const,
      color: "text-orange-500",
      activeBorder: "data-[state=active]:border-orange-500",
      description: "This action will only affect the Publish environment.",
      detailedDescription: "Reversal process that removes content from the live Publish environment while keeping it available in Author for future edits. This action triggers a localized index update to reflect the change immediately."
    }
  ];

  return (
    <LoadProvider checkIsNotUndefined={sources} error={error} tryAgainUrl={`${ROUTES.INTEGRATION_INSTANCE}/${id}`} >
      <SubPageHeader icon={IconAdjustmentsSearch} feature="Indexing Manager" name="Indexing Manager" description="Directly manage and override content indexing states to ensure search accuracy." />
      <div className="w-full mx-auto mt-6 px-6">
        <Tabs defaultValue="INDEXING" className="w-full">
          <TabsList className="w-full h-auto bg-transparent p-0 gap-4 flex-wrap justify-start">
            {items.map((item) => (
              <TabsTrigger
                key={item.value}
                value={item.value}
                className={`flex-1 min-w-37.5 flex flex-col items-center gap-3 py-6 h-auto border-2 border-transparent bg-card shadow-sm hover:bg-accent hover:text-accent-foreground data-[state=active]:shadow-md transition-all ${item.activeBorder}`}
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
                    <HoverCard>
                      <HoverCardTrigger asChild>
                        <IconInfoCircle className="w-5 h-5 text-muted-foreground cursor-help" />
                      </HoverCardTrigger>
                      <HoverCardContent className="w-80">
                        <div className="space-y-2">
                          <h4 className="font-medium leading-none">About {item.title}</h4>
                          <p className="text-sm text-muted-foreground">
                            {item.detailedDescription}
                          </p>
                        </div>
                      </HoverCardContent>
                    </HoverCard>
                  </h3>
                  <p className="text-muted-foreground text-sm mt-1">
                    Configure the parameters for the {item.title.toLowerCase()} operation below.
                  </p>
                  <div className="mt-2 rounded-md bg-blue-50 p-4 text-sm text-blue-700 dark:bg-blue-900/20 dark:text-blue-400">
                    {item.description}
                  </div>
                </div>
                <IntegrationIndexingManagerForm integrationId={id} mode={item.mode} sources={sources} />
              </TabsContent>
            ))}
          </div>
        </Tabs>
      </div>
    </LoadProvider>
  )
}
