import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';
import {TurLoggingGeneral} from "../../model/logging-general.model";
import {Observable} from "rxjs";
import {TurLLMInstance} from "../../../llm/model/llm-instance.model";
import {TurLoggingService} from "../../service/logging.service";

@Component({
    selector: 'logging-aem-page',
    templateUrl: './logging-aem-page.component.html',
    standalone: false
})
export class TurLoggingAemPageComponent implements OnInit {
  private turLoggingAemList : Observable<TurLoggingGeneral[]>
  constructor(private readonly notifier: NotifierService,
              private router: Router,
              private turLoggingService: TurLoggingService) {
    this.turLoggingAemList = turLoggingService.aem();
  }

  getLoggingAemList(): Observable<TurLoggingGeneral[]> {
    return this.turLoggingAemList;
  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
