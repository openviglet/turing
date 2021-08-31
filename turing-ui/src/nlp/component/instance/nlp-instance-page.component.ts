import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { TurNLPInstance } from '../../model/nlp-instance.model';
import { NotifierService } from 'angular-notifier';
import { TurNLPInstanceService } from '../../service/nlp-instance.service';
import { ActivatedRoute } from '@angular/router';
import { TurNLPVendor } from 'src/nlp/model/nlp-vendor.model';
import { TurNLPVendorService } from 'src/nlp/service/nlp-vendor.service';
import { TurLocale } from 'src/locale/model/locale.model';
import { TurLocaleService } from 'src/locale/service/locale.service';
import { FormControl, Validators } from '@angular/forms';
import { NgxSmartModalService } from 'ngx-smart-modal';

@Component({
  selector: 'nlp-instance-page',
  templateUrl: './nlp-instance-page.component.html'
})
export class TurNLPInstancePageComponent implements OnInit {
  @ViewChild('modalDelete') modalDelete: ElementRef;
  private turNLPInstance: Observable<TurNLPInstance>;
  private turLocales: Observable<TurLocale[]>;
  private turNLPVendors: Observable<TurNLPVendor[]>;

  portControl = new FormControl(80, [Validators.max(100), Validators.min(0)])


  constructor(private readonly notifier: NotifierService,
    private turNLPInstanceService: TurNLPInstanceService,
    private turLocaleService: TurLocaleService,
    private turNLPVendorService: TurNLPVendorService,
    private route: ActivatedRoute,
    public ngxSmartModalService: NgxSmartModalService) {
    this.turNLPVendors = turNLPVendorService.query()
    this.turLocales = turLocaleService.query()
    let id = this.route.snapshot.paramMap.get('id');
    this.turNLPInstance = this.turNLPInstanceService.get(id);

  }

  getTurNLPInstance(): Observable<TurNLPInstance> {
    return this.turNLPInstance;
  }

  getTurNLPVendors(): Observable<TurNLPVendor[]> {

    return this.turNLPVendors;
  }


  getTurLocales(): Observable<TurLocale[]> {

    return this.turLocales;
  }

  ngOnInit(): void {
  }

  public save(_turNLPInstance: TurNLPInstance) {
    this.turNLPInstanceService.save(_turNLPInstance).subscribe(
      (turNLPInstance: TurNLPInstance) => {
        _turNLPInstance = turNLPInstance;
        this.notifier.notify("success", turNLPInstance.title.concat(" NLP instance was updated."));
      },
      response => {
        this.notifier.notify("error", "NLP instance was error: " + response);
      },
      () => {
        // console.log('The POST observable is now completed.');
      });
  }

  public delete(_turNLPInstance: TurNLPInstance) {
    this.turNLPInstanceService.delete(_turNLPInstance).subscribe(
      (turNLPInstance: TurNLPInstance) => {
        _turNLPInstance = turNLPInstance;
        this.notifier.notify("success", turNLPInstance.title.concat(" NLP instance was deleted."));
        this.modalDelete.nativeElement.removeAttribute("open");
      },
      response => {
        this.notifier.notify("error", "NLP instance was error: " + response);
      },
      () => {
        // console.log('The POST observable is now completed.');
      });
  }
}
