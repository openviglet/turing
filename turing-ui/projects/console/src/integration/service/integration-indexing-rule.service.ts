/*
 * Copyright (C) 2016-2020 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {TurIntegrationIndexingRule} from "../model/integration-indexing-rule.model";

@Injectable()
export class TurIntegrationIndexingRuleService {
  private integrationId: string | undefined;
  private endpointPrefix: string | undefined;
  constructor(private httpClient: HttpClient) { }
  setIntegrationId(integrationId: string) {
    this.integrationId = integrationId;
    this.endpointPrefix = `${environment.apiUrl}/api/v2/integration/${this.integrationId}/connector`
  }

  queryAll(): Observable<TurIntegrationIndexingRule[]> {
    return this.httpClient.get<TurIntegrationIndexingRule[]>(`${this.endpointPrefix}/indexing-rule`);
  }

  get(id: string): Observable<TurIntegrationIndexingRule> {
    return this.httpClient.get<TurIntegrationIndexingRule>(`${this.endpointPrefix}/indexing-rule/${id}`);
  }

  getStructure(): Observable<TurIntegrationIndexingRule> {
    return this.httpClient.get<TurIntegrationIndexingRule>(`${this.endpointPrefix}/indexing-rule/structure`);
  }

  public save(turIntegrationIndexingRule: TurIntegrationIndexingRule, newObject: boolean): Observable<TurIntegrationIndexingRule> {
    if (newObject) {
      return this.httpClient.post<TurIntegrationIndexingRule>(`${this.endpointPrefix}/indexing-rule`,
        JSON.stringify(turIntegrationIndexingRule));
    }
    else {
      return this.httpClient.put<TurIntegrationIndexingRule>(`${this.endpointPrefix}/indexing-rule/${turIntegrationIndexingRule.id}`,
        JSON.stringify(turIntegrationIndexingRule));
    }
  }

  public delete( turIntegrationIndexingRule: TurIntegrationIndexingRule): Observable<TurIntegrationIndexingRule> {
    return this.httpClient
      .delete<TurIntegrationIndexingRule>(`${environment.apiUrl}/api/v2/integration/${this.integrationId}/connector/indexing-rule/${turIntegrationIndexingRule.id}`);
  }
}
