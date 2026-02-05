import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import { SubPageHeader } from "@/components/sub.page.header";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurIntegrationIndexingRule } from "@/models/integration/integration-indexing-rule.model";
import { TurIntegrationIndexingRuleService } from "@/services/integration/integration-indexing-rule.service";
import { IconGitCommit } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export default function IntegrationInstanceIndexingRulesListPage() {
  const { id } = useParams<{ id: string }>();
  const [integrationIndexingRules, setIntegrationIndexingRules] = useState<TurIntegrationIndexingRule[]>();
  const turIntegrationIndexingRuleService = new TurIntegrationIndexingRuleService(id || "");
  useEffect(() => {
    turIntegrationIndexingRuleService.query().then(setIntegrationIndexingRules)
  }, [id])
  const gridItemList = useGridAdapter(integrationIndexingRules, {
    name: "name",
    description: "description",
    url: (item) => `${ROUTES.INTEGRATION_INSTANCE}/${id}/indexing-rule/${item.id}`
  });
  return (
    <>

      {gridItemList.length > 0 ? (<>
        <SubPageHeader icon={IconGitCommit} name="Indexing Rules" feature="Indexing Rules"
          description="Establish guidelines for how the indexing process will operate."
          urlNew={`${ROUTES.INTEGRATION_INSTANCE}/${id}/indexing-rule/new`} />
        <GridList gridItemList={gridItemList} />
      </>) : (
        <BlankSlate
          icon={IconGitCommit}
          title="You don’t seem to have any Indexing Rules."
          description="Create a new indexing rule to define how AEM sources are indexed."
          buttonText="New Indexing Rule"
          urlNew={`${ROUTES.INTEGRATION_INSTANCE}/${id}/indexing-rule/new`} />
      )}
    </>
  )
}
