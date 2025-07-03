import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {NotifierService} from 'angular-notifier-updated';
import {ActivatedRoute, Router} from '@angular/router';
import {UntypedFormControl, Validators} from '@angular/forms';
import {TurIntegrationAemSource} from "../../../model/integration-aem-source.model";
import {TurIntegrationAemSourceService} from "../../../service/integration-aem-source.service";
import {AceComponent, AceConfigInterface, AceDirective} from "ngx-ace-wrapper";
import {TurIntegrationAemLocalePath} from "../../../model/integration-aem-locale-path.model";
import {TurLocale} from "../../../../locale/model/locale.model";
import {TurLocaleService} from "../../../../locale/service/locale.service";
import {TurIntegrationConnectorService} from "../../../service/integration-connector.service";

@Component({
    selector: 'integration-aem-page',
    templateUrl: './integration-aem-source.component.html',
    standalone: false
})

export class TurIntegrationAemSourceComponent implements OnInit {
  public config: AceConfigInterface = {
    mode: 'ace/mode/json',
    theme: 'github',
    readOnly: false,
  };
  @ViewChild(AceComponent, {static: false})
  componentRef?: AceComponent;
  @ViewChild(AceDirective, {static: false})
  directiveRef?: AceDirective;
  @ViewChild('modalDelete')
  modalDelete!: ElementRef;
  private readonly turIntegrationAemSource: Observable<TurIntegrationAemSource>;
  private readonly newObject: boolean = false;
  private readonly integrationId: string;
  private readonly turLocales: Observable<TurLocale[]>;
  portControl = new UntypedFormControl(80, [Validators.max(100),
    Validators.min(0)])


  constructor(
    private readonly notifier: NotifierService,
    private turIntegrationConnectorService: TurIntegrationConnectorService,
    private turIntegrationAemSourceService: TurIntegrationAemSourceService,
    private turLocaleService: TurLocaleService,
    private activatedRoute: ActivatedRoute,
    private router: Router) {
    this.config.mode = 'json';
    this.config.tabSize = 2;
    this.config.wrap = true;
    this.turLocales = turLocaleService.query();
    let id: string = this.activatedRoute.snapshot.paramMap.get('aemId') || "";
    this.integrationId = this.activatedRoute.parent?.snapshot.paramMap.get('id') || "";
    turIntegrationAemSourceService.setIntegrationId(this.integrationId);
    this.newObject = (id != null && id.toLowerCase() === 'new');

    this.turIntegrationAemSource = this.newObject ? this.turIntegrationAemSourceService.getStructure() :
      this.turIntegrationAemSourceService.get(id);
  }

  getIntegrationId(): string {
    return this.integrationId;
  }
  isNewObject(): boolean {
    return this.newObject;
  }

  saveButtonCaption(): string {
    return this.newObject ? "Create AEM source" : "Update AEM source";
  }

  getTurIntegrationAemSource(): Observable<TurIntegrationAemSource> {
    return this.turIntegrationAemSource;
  }


  ngOnInit(): void {
  }

  newLocale(turIntegrationAemLocalePaths: TurIntegrationAemLocalePath[]) {
    let turIntegrationAemLocalePath: TurIntegrationAemLocalePath = new TurIntegrationAemLocalePath();
    turIntegrationAemLocalePaths.push(turIntegrationAemLocalePath);

  }

  removeLocale(turIntegrationAemSource: TurIntegrationAemSource,
               turIntegrationAemLocalePath: TurIntegrationAemLocalePath) {
    turIntegrationAemSource.localePaths =
      turIntegrationAemSource.localePaths.filter(condition =>
        condition != turIntegrationAemLocalePath)
  }

  getTurLocales(): Observable<TurLocale[]> {
    return this.turLocales;
  }

  public save(_turIntegrationAemSource: TurIntegrationAemSource) {
    this.turIntegrationAemSourceService.save(_turIntegrationAemSource, this.newObject).subscribe(
      (turIntegrationAemSource: TurIntegrationAemSource) => {
        let message: string = this.newObject ?
          " Integration AEM source was created." :
          " Integration AEM source was updated.";

        _turIntegrationAemSource = turIntegrationAemSource;

        this.notifier.notify("success", turIntegrationAemSource.name.concat(message));

        this.router.navigate(['/integration/instance', this.getIntegrationId(), 'aem']);
      },
      (response: string) => {
        this.notifier.notify("error", "Integration AEM source was error: " + response);
      },
      () => {
        // console.log('The POST observable is now completed.');
      });
  }

  public delete(_turIntegrationAemSource: TurIntegrationAemSource) {
    this.turIntegrationAemSourceService.delete(_turIntegrationAemSource).subscribe(
      () => {
        this.notifier.notify("success", _turIntegrationAemSource.name
          .concat(" Integration AEM source was deleted."));
        this.modalDelete.nativeElement.removeAttribute("open");
        this.router.navigate(['/integration/instance', this.getIntegrationId(), 'aem']);
      },
      (response: string) => {
        this.notifier.notify("error", "Integration AEM source was error: " + response);
      });
  }

  indexAll(_turIntegrationAemSource: TurIntegrationAemSource) {
    this.turIntegrationConnectorService.indexAll(_turIntegrationAemSource).subscribe(
      () => {
        this.notifier.notify("success", _turIntegrationAemSource.name
          .concat(" Integration AEM source is indexing all content."));
        this.modalDelete.nativeElement.removeAttribute("open");
        this.router.navigate(['/integration/instance', this.getIntegrationId(), 'aem']);
      },
      (response: string) => {
        this.notifier.notify("error", "Integration AEM source was error: " + response);
      });
  }
  reindexAll(_turIntegrationAemSource: TurIntegrationAemSource) {
    this.turIntegrationConnectorService.reindexAll(_turIntegrationAemSource).subscribe(
      () => {
        this.notifier.notify("success", _turIntegrationAemSource.name
          .concat(" Integration AEM source is reindexing all content."));
        this.modalDelete.nativeElement.removeAttribute("open");
        this.router.navigate(['/integration/instance', this.getIntegrationId(), 'aem']);
      },
      (response: string) => {
        this.notifier.notify("error", "Integration AEM source was error: " + response);
      });
  }
}
