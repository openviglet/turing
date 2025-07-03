/*
 * Copyright (C) 2016-2024 the original author or authors.
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
import {TurIntegrationAemSource} from "../model/integration-aem-source.model";
import { environment } from '../../../../../environments/environment';

@Injectable()
export class TurIntegrationConnectorService {
  private integrationId: string | undefined;
  constructor(private httpClient: HttpClient) {}

    private getConnectorUrl() {
    return `${environment.apiUrl}/api/v2/integration/${this.integrationId}/connector`;
  }

  indexAll(turIntegrationAemSource: TurIntegrationAemSource) {
    return this.httpClient.get(`${this.getConnectorUrl()}/${turIntegrationAemSource.id}/indexAll`);
  }

  reindexAll(turIntegrationAemSource: TurIntegrationAemSource) {
    return this.httpClient.get(`${this.getConnectorUrl()}/${turIntegrationAemSource.id}/reindexAll`);
  }
}
