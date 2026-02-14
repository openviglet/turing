import axios from "axios";
import type { TurSNChat } from "../models/sn-chat.model";
import type { TurSNSearch } from "../models/sn-search.model";

interface QueryOptions {
  q: string;
  p?: string;
  _setlocale?: string;
  sort?: string;
  fq?: string[];
  tr?: string[];
  nfpr?: string;
}

export class TurSNSearchService {
  static query(
    turSiteName: string,
    options: QueryOptions,
  ): Promise<TurSNSearch> {
    const queryString = this.generateQueryString(
      options.q,
      options.p || "",
      options._setlocale || "",
      options.sort || "",
      options.fq || [],
      options.tr || [],
      options.nfpr || "",
    );
    return axios
      .get<TurSNSearch>(`/sn/${turSiteName}/search?${queryString}`)
      .then((response) => response.data);
  }

  static chat(
    turSiteName: string,
    q: string,
    _setlocale: string,
  ): Promise<TurSNChat> {
    return axios
      .get<TurSNChat>(
        `/api/sn/${turSiteName}/chat?q=${q}&_setlocale=${_setlocale}`,
      )
      .then((response) => response.data);
  }

  static autoComplete(
    turSiteName: string,
    q: string,
    p: string,
    _setlocale: string,
    sort: string,
    fq: string[],
    tr: string[],
    nfpr: string,
  ): Promise<string[]> {
    const queryString = this.generateQueryString(
      q,
      p,
      _setlocale,
      sort,
      fq,
      tr,
      nfpr,
    );
    return axios
      .get<string[]>(`/api/sn/${turSiteName}/ac?${queryString}`)
      .then((response) => response.data);
  }

  static generateQueryString(
    q: string,
    p: string,
    _setlocale: string,
    sort: string,
    fq: string[],
    tr: string[],
    nfpr: string,
  ): string {
    let queryString = "";

    if (q) {
      queryString += `q=${encodeURIComponent(q)}`;
    } else {
      queryString += `q=*`;
    }

    if (p) {
      queryString += `&p=${p}`;
    } else {
      queryString += `&p=1`;
    }

    if (_setlocale) {
      queryString += `&_setlocale=${_setlocale}`;
    }

    if (sort) {
      queryString += `&sort=${sort}`;
    } else {
      queryString += `&sort=relevance`;
    }

    if (fq) {
      if (Array.isArray(fq)) {
        fq.forEach(function (fqItem) {
          queryString += `&fq[]=${encodeURIComponent(fqItem)}`;
        });
      } else {
        queryString += `&fq[]=${encodeURIComponent(fq)}`;
      }
    }

    if (tr) {
      if (Array.isArray(tr)) {
        tr.forEach(function (trItem) {
          queryString += `&tr[]=${encodeURIComponent(trItem)}`;
        });
      } else {
        queryString += `&tr[]=${encodeURIComponent(tr)}`;
      }
    }

    if (nfpr) {
      queryString += `&nfpr=${encodeURIComponent(nfpr)}`;
    }

    return queryString;
  }
}
