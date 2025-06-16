import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';

import {Observable} from "rxjs";
import {TurLoggingService} from "../../service/logging.service";
import {TurLoggingIndexing} from "../../model/logging-indexing.model";

@Component({
  selector: 'logging-indexing-page',
  templateUrl: './logging-indexing-page.component.html',
  standalone: false
})
export class TurLoggingIndexingPageComponent implements OnInit {
  private turLoggingIndexingList : Observable<TurLoggingIndexing[]>
  constructor(private readonly notifier: NotifierService,
              private router: Router,
              private turLoggingService: TurLoggingService) {
    this.turLoggingIndexingList = turLoggingService.indexing();
  }

  getLoggingIndexingList(): Observable<TurLoggingIndexing[]> {
    return this.turLoggingIndexingList;
  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
