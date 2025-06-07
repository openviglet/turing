import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';

@Component({
    selector: 'integration-root-page',
    templateUrl: './integration-monitoring-page.component.html',
    standalone: false
})
export class TurIntegrationMonitoringPageComponent implements OnInit {

  constructor(private readonly notifier: NotifierService, private router: Router) {

  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
