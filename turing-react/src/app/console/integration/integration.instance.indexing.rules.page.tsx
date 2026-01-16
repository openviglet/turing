import { IntegrationIndexingRulesForm } from "@/components/integration/integration.indexing.rules.form";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurIntegrationIndexingRule } from "@/models/integration/integration-indexing-rule.model";
import { TurIntegrationIndexingRuleService } from "@/services/integration/integration-indexing-rule.service";
import { IconTools } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export default function IntegrationInstanceIndexingRulePage() {
  const { id, ruleId } = useParams() as { id: string, ruleId: string };
  const [integrationIndexingRules, setIntegrationIndexingRules] = useState<TurIntegrationIndexingRule>({} as TurIntegrationIndexingRule);
  const turIntegrationIndexingRuleService = new TurIntegrationIndexingRuleService(id || "");
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turIntegrationIndexingRuleService.get(ruleId).then(setIntegrationIndexingRules);
      setIsNew(false);
    }
  }, [id])
  return (
    <>
      <SubPageHeader icon={IconTools} title="Indexing Rules" description="Establish guidelines for how the indexing process will operate." />
      <IntegrationIndexingRulesForm value={integrationIndexingRules} integrationId={id} isNew={isNew} />
    </>
  )
}
