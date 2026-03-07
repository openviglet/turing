import type { TurLLMInstance } from "@/models/llm/llm-instance.model.ts";
import axios from "axios";

export interface ChatMessageItem {
  role: "user" | "assistant";
  content: string;
}

export interface ChatResponse {
  role: string;
  content: string;
}

export class TurChatService {
  async send(
    llmInstanceId: string,
    messages: ChatMessageItem[],
  ): Promise<ChatResponse> {
    const response = await axios.post<ChatResponse>(
      `/v2/llm/${llmInstanceId}/chat`,
      { messages },
    );
    return response.data;
  }

  async queryLLMInstances(): Promise<TurLLMInstance[]> {
    const response = await axios.get<TurLLMInstance[]>("/llm");
    return response.data;
  }
}
