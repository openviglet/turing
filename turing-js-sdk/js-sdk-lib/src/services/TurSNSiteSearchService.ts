import axios, { AxiosInstance, AxiosRequestConfig } from "axios";
import { TurSNSearchLatestRequest } from "../models/TurSNSearchLatestRequest.js";
import { TurSNSiteLocale } from "../models/TurSNSiteLocale.js";
import { TurSNSitePostParams } from "../models/TurSNSitePostParams.js";
import { TurSNSiteSearch } from "../models/TurSNSiteSearch.js";
import { TurChatMessage } from "../models/TurChatMessage.js";
import { TurSNSearchParams } from "../types/TurSNSearchParams.js";

/**
 * Service class for Turing Semantic Navigation Site Search API
 */
export class TurSNSiteSearchService {
  private axiosInstance: AxiosInstance;

  constructor(baseURL?: string, config?: AxiosRequestConfig) {
    this.axiosInstance = axios.create({
      baseURL: baseURL || "",
      ...config,
    });
  }

  /**
   * Configure the axios instance
   */
  public configure(config: AxiosRequestConfig): void {
    this.axiosInstance = axios.create({
      ...this.axiosInstance.defaults,
      ...config,
    });
  }

  /**
   * Set the base URL
   */
  public setBaseURL(baseURL: string): void {
    this.axiosInstance.defaults.baseURL = baseURL;
  }

  /**
   * Set authorization header
   */
  public setAuth(token: string): void {
    this.axiosInstance.defaults.headers.common[
      "Authorization"
    ] = `Bearer ${token}`;
  }

  /**
   * GET search list - returns list of search result identifiers
   */
  public async searchList(
    siteName: string,
    params?: TurSNSearchParams
  ): Promise<Set<string>> {
    const response = await this.axiosInstance.get<string[]>(
      `/api/sn/${siteName}/search/list`,
      { params: this.normalizeParams(params) }
    );
    return new Set(response.data);
  }

  /**
   * GET search - returns full search results
   */
  public async search(
    siteName: string,
    params?: TurSNSearchParams
  ): Promise<TurSNSiteSearch> {
    const response = await this.axiosInstance.get<TurSNSiteSearch>(
      `/api/sn/${siteName}/search`,
      { params: this.normalizeParams(params) }
    );
    return response.data;
  }

  /**
   * POST search - advanced search with body parameters
   */
  public async searchPost(
    siteName: string,
    postParams: TurSNSitePostParams,
    params?: TurSNSearchParams
  ): Promise<TurSNSiteSearch> {
    const response = await this.axiosInstance.post<TurSNSiteSearch>(
      `/api/sn/${siteName}/search`,
      postParams,
      { params: this.normalizeParams(params) }
    );
    return response.data;
  }

  /**
   * GET locales - returns available locales for the site
   */
  public async getLocales(siteName: string): Promise<TurSNSiteLocale[]> {
    const response = await this.axiosInstance.get<TurSNSiteLocale[]>(
      `/api/sn/${siteName}/search/locales`
    );
    return response.data;
  }

  /**
   * POST latest searches - returns latest search queries
   */
  public async getLatestSearches(
    siteName: string,
    rows?: number,
    locale?: string,
    request?: TurSNSearchLatestRequest
  ): Promise<string[]> {
    const params: any = {};
    if (rows !== undefined) params.rows = rows;
    if (locale !== undefined) params.locale = locale;

    const response = await this.axiosInstance.post<string[]>(
      `/api/sn/${siteName}/search/latest`,
      request,
      { params }
    );
    return response.data;
  }

  /**
   * GET chat message - returns a chat response from the Generative AI API
   */
  public async chatMessage(
    siteName: string,
    query: string,
    localeRequest?: string
  ): Promise<TurChatMessage> {
    const params: any = { q: query };
    if (localeRequest !== undefined) params._setlocale = localeRequest;

    const response = await this.axiosInstance.get<TurChatMessage>(
      `/api/sn/${siteName}/chat`,
      { params }
    );
    return response.data;
  }

  /**
   * Normalize parameters for API calls
   */
  private normalizeParams(params?: TurSNSearchParams): any {
    if (!params) return {};

    const normalized: any = {};

    if (params.q !== undefined) normalized.q = params.q;
    if (params.currentPage !== undefined) normalized.p = params.currentPage;
    if (params.filterQueriesDefault !== undefined)
      normalized.fq = params.filterQueriesDefault;
    if (params.filterQueriesAnd !== undefined)
      normalized.fqAnd = params.filterQueriesAnd;
    if (params.filterQueriesOr !== undefined)
      normalized.fqOr = params.filterQueriesOr;
    if (params.fqOperator !== undefined)
      normalized.fqOperator = params.fqOperator;
    if (params.fqItemOperator !== undefined)
      normalized.fqItemOperator = params.fqItemOperator;
    if (params.sort !== undefined) normalized.sort = params.sort;
    if (params.rows !== undefined) normalized.rows = params.rows;
    if (params.group !== undefined) normalized.group = params.group;
    if (params.autoCorrectionDisabled !== undefined)
      normalized.autoCorrectionDisabled = params.autoCorrectionDisabled;
    if (params.localeRequest !== undefined)
      normalized._setlocale = params.localeRequest;

    return normalized;
  }
}
