import { TurSNSiteSearchDocumentMetadata } from './TurSNSiteSearchDocumentMetadata.js';

/**
 * Search result document
 */
export interface TurSNSiteSearchDocument {
  source?: string;
  elevate?: boolean;
  metadata?: TurSNSiteSearchDocumentMetadata[];
  fields?: { [key: string]: any };
}