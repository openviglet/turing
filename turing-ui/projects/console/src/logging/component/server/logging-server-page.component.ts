import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';
import {TurLoggingGeneral} from "../../model/logging-general.model";
import {Observable} from "rxjs";
import {TurLLMInstance} from "../../../llm/model/llm-instance.model";
import {TurLoggingService} from "../../service/logging.service";

@Component({
    selector: 'logging-server-page',
    templateUrl: './logging-server-page.component.html',
    standalone: false
})
export class TurLoggingServerPageComponent implements OnInit {
  private turLoggingServerList : Observable<TurLoggingGeneral[]>
  constructor(private readonly notifier: NotifierService,
              private router: Router,
              private turLoggingService: TurLoggingService) {
    this.turLoggingServerList = turLoggingService.server();
  }

  getLoggingServerList(): Observable<TurLoggingGeneral[]> {
    return this.turLoggingServerList;
  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
