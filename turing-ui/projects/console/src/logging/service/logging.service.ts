/*
 * Copyright (C) 2016-2021 the original author or authors.
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
import {TurLoggingGeneral} from "../model/logging-general.model";
import {TurLoggingIndexing} from "../model/logging-indexing.model";

@Injectable()
export class TurLoggingService {

  constructor(private httpClient: HttpClient) { }

  general(): Observable<TurLoggingGeneral[]> {
    return this.httpClient.get<TurLoggingGeneral[]>(`${environment.apiUrl}/api/logging`);
  }

  indexing(): Observable<TurLoggingIndexing[]> {
    return this.httpClient.get<TurLoggingIndexing[]>(`${environment.apiUrl}/api/logging/indexing`);
  }

}
