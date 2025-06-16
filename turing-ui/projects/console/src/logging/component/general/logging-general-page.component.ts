import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';
import {TurLoggingGeneral} from "../../model/logging-general.model";
import {Observable} from "rxjs";
import {TurLLMInstance} from "../../../llm/model/llm-instance.model";
import {TurLoggingService} from "../../service/logging.service";

@Component({
    selector: 'logging-general-page',
    templateUrl: './logging-general-page.component.html',
    standalone: false
})
export class TurLoggingGeneralPageComponent implements OnInit {
  private turLoggingGeneralList : Observable<TurLoggingGeneral[]>
  constructor(private readonly notifier: NotifierService,
              private router: Router,
              private turLoggingService: TurLoggingService) {
    this.turLoggingGeneralList = turLoggingService.general();
  }

  getLoggingGeneralList(): Observable<TurLoggingGeneral[]> {
    return this.turLoggingGeneralList;
  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
