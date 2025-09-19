/**
 * Represents a chat message response from the Generative AI API
 */
export interface TurChatMessage {
  /**
   * Indicates if the GenerativeAI feature is enabled
   */
  enabled: boolean;
  
  /**
   * The text content of the chat message
   */
  text: string;
}