import type { TurLLMInstance } from "./llm-instance.model";
import type { TurStoreInstance } from "./store-instance.model";

export interface TurSNSiteGenAi {
  id: string;
  turLLMInstance: TurLLMInstance;
  turStoreInstance: TurStoreInstance;
  enabled: boolean;
  systemPrompt: string;
}
