import {Component} from '@angular/core';
import {NotifierService} from 'angular-notifier-updated';
import {ActivatedRoute} from '@angular/router';
import {Observable} from "rxjs";
import {TurIntegrationMonitoringService} from "../../../service/integration-monitoring.service";
import {TurIntegrationMonitoring} from "../../../model/integration-monitoring.model";

@Component({
  selector: 'integration-root-page',
  templateUrl: './integration-monitoring-page.component.html',
  standalone: false
})
export class TurIntegrationMonitoringPageComponent {
  private turIntegrationIndexing: Observable<TurIntegrationMonitoring>;
  private integrationId: string;
  private source: string;
  private currentSource: string;

  constructor(
    private readonly notifier: NotifierService,
    private turIntegrationMonitoringService: TurIntegrationMonitoringService,
    private activatedRoute: ActivatedRoute) {
    this.integrationId = this.activatedRoute.parent?.snapshot.paramMap.get('id') || "";
    this.turIntegrationMonitoringService.setIntegrationId(this.integrationId);
    this.source = this.activatedRoute.snapshot.paramMap.get('source') || "";
    this.currentSource = this.source;
    this.turIntegrationIndexing = this.getMonitoringService();
  }

  getIntegrationId(): string {
    return this.integrationId;
  }

  getMonitoringService(): Observable<TurIntegrationMonitoring> {
    if (this.source == "all") {
      return this.turIntegrationMonitoringService.queryAll();
    } else {
      return this.turIntegrationMonitoringService.get(this.source);
    }
  }

  getTurIntegrationMonitoring(): Observable<TurIntegrationMonitoring> {
    this.source = this.activatedRoute.snapshot.paramMap.get('source') || "";
    if (this.currentSource != this.source) {
      this.currentSource = this.source;
      this.turIntegrationIndexing = this.getMonitoringService();
    }
    return this.turIntegrationIndexing;
  }
}
