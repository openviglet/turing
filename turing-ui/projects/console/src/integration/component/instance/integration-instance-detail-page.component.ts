import {Component, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {TurIntegrationInstance} from '../../model/integration-instance.model';
import {NotifierService} from 'angular-notifier-updated';
import {TurIntegrationInstanceService} from '../../service/integration-instance.service';
import {ActivatedRoute, Router} from '@angular/router';
import {TurIntegrationVendor} from '../../model/integration-vendor.model';
import {TurIntegrationVendorService} from '../../service/integration-vendor.service';

@Component({
  selector: 'integration-instance-detail-page',
  templateUrl: './integration-instance-detail-page.component.html',
  standalone: false
})
export class TurIntegrationInstanceDetailPageComponent implements OnInit {
  private readonly turIntegrationInstance: Observable<TurIntegrationInstance>;
  private readonly turIntegrationVendors: Observable<TurIntegrationVendor[]>;
  private readonly newObject: boolean = false;

  constructor(
    private readonly notifier: NotifierService,
    private turIntegrationInstanceService: TurIntegrationInstanceService,
    turIntegrationVendorService: TurIntegrationVendorService,
    private activatedRoute: ActivatedRoute,
    private router: Router) {

    this.turIntegrationVendors = turIntegrationVendorService.query();

    let id: string = this.activatedRoute.snapshot.paramMap.get('id') ||
      this.activatedRoute.parent?.snapshot.paramMap.get('id') || "";

    this.newObject = (id != null && id.toLowerCase() === 'new');

    this.turIntegrationInstance = this.newObject ?
      this.turIntegrationInstanceService.getStructure() :
      this.turIntegrationInstanceService.get(id);
  }

  saveButtonCaption(): string {
    return this.newObject ? "Create integration instance" : "Update integration instance";
  }

  getTurIntegrationInstance(): Observable<TurIntegrationInstance> {
    return this.turIntegrationInstance;
  }

  getTurIntegrationVendors(): Observable<TurIntegrationVendor[]> {

    return this.turIntegrationVendors;
  }

  ngOnInit(): void {
  }

  public save(_turIntegrationInstance: TurIntegrationInstance) {
    this.turIntegrationInstanceService.save(_turIntegrationInstance, this.newObject).subscribe(
      (turIntegrationInstance: TurIntegrationInstance) => {
        let message: string = this.newObject ? " Integration instance was created." : " Integration instance was updated.";

        _turIntegrationInstance = turIntegrationInstance;

        this.notifier.notify("success", turIntegrationInstance.title.concat(message));

        this.router.navigate(['/integration/instance']);
      },
      response => {
        this.notifier.notify("error", "Integration instance was error: " + response);
      },
      () => {
        // console.log('The POST observable is now completed.');
      });
  }
}
