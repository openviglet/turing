import { Component, OnInit } from '@angular/core';
import { NotifierService } from 'angular-notifier-updated';
import { Router, RouterModule } from '@angular/router';

@Component({
    selector: 'logging-root-page',
    templateUrl: './logging-root-page.component.html',
    standalone: false
})
export class TurLoggingRootPageComponent implements OnInit {

  constructor(private readonly notifier: NotifierService, private router: Router) {

  }

  getRouter(): Router {
    return this.router;
  }

  ngOnInit(): void {
  }
}
