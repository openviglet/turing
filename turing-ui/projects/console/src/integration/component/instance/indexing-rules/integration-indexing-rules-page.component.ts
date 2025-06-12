import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {NotifierService} from 'angular-notifier-updated';
import {ActivatedRoute, Router} from '@angular/router';
import {TurSNSiteField} from "../../../../sn/model/sn-site-field.model";
import {TurIntegrationIndexingRule} from "../../../model/integration-indexing-rule.model";
import {TurIntegrationIndexingRuleService} from "../../../service/integration-indexing-rule.service";

@Component({
    selector: 'integration-indexing-rules-page',
    templateUrl: './integration-indexing-rules-page.component.html',
    standalone: false
})
export class TurIntegrationIndexingRulesPageComponent implements OnInit {
  @ViewChild('modalDeleteRankingExpression')
  modalDelete!: ElementRef;
  private readonly integrationIndexingRule: Observable<TurIntegrationIndexingRule>;
  private turSNSiteSEFields: TurSNSiteField[] = new Array<TurSNSiteField>;
  private readonly newObject: boolean = false;
  private integrationId: string;
  private indexingRuleId: string;

  constructor(
    private readonly notifier: NotifierService,
    private integrationIndexingRuleService: TurIntegrationIndexingRuleService,

    private activatedRoute: ActivatedRoute,
    private router: Router) {
    this.indexingRuleId = this.activatedRoute.snapshot.paramMap.get('indexingRuleId') || "";
    this.integrationId = this.activatedRoute.parent?.parent?.snapshot.paramMap.get('id') || "";
    this.newObject = (this.indexingRuleId.toLowerCase() === 'new');
    this.integrationIndexingRuleService.setIntegrationId(this.integrationId);
    this.integrationIndexingRule = this.newObject ? this.integrationIndexingRuleService.getStructure() :
      this.integrationIndexingRuleService.get(this.indexingRuleId);
  }

  getIntegrationIndexingRule(): Observable<TurIntegrationIndexingRule> {
    return this.integrationIndexingRule;
  }

  getTurSNSiteSEFields(): TurSNSiteField[] {
    return this.turSNSiteSEFields;
  }

  getFieldType(fieldName: string): string {
    return <string>this.turSNSiteSEFields.find(field => field.name == fieldName)?.type;
  }

  newValue(values: string[]) {
    if (values == null) {
      values = [];
    }
    let value = "";
        values.push(value);

  }

  removeValue(integrationIndexingRule: TurIntegrationIndexingRule, _value: string) {
    integrationIndexingRule.values =
      integrationIndexingRule.values.filter(value =>
        value != _value)
  }

  ngOnInit(): void {
    // Empty
  }

  isNewObject(): boolean {
    return this.newObject;
  }
  trackByIndex(index: number, obj: any): any {
    return index;
  }
  saveButtonCaption(): string {
    return this.newObject ? "Create rule" : "Update rule";
  }

  public save(_integrationIndexingRule: TurIntegrationIndexingRule) {
    this.integrationIndexingRuleService.save(_integrationIndexingRule, this.newObject).subscribe(
      (integrationIndexingRule: TurIntegrationIndexingRule) => {
        let message: string = this.newObject ? " indexing rule was created." : " indexing rule was updated.";
        _integrationIndexingRule = integrationIndexingRule;
        this.notifier.notify("success", integrationIndexingRule.name.concat(message));
        this.router.navigate(['/sn/site/', this.indexingRuleId, 'indexing-rule', 'list']);
      },
      response => {
        this.notifier.notify("error", "Indexing Rule was error: " + response);
      },
      () => {
        // The POST observable is now completed.
      });
  }

  public delete(_integrationIndexingRule: TurIntegrationIndexingRule) {
    this.integrationIndexingRuleService.delete(_integrationIndexingRule).subscribe(
      (integrationIndexingRule: TurIntegrationIndexingRule) => {
        this.notifier.notify("success", _integrationIndexingRule.name.concat(" indexing rule was deleted."));
        this.modalDelete.nativeElement.removeAttribute("open");
        this.router.navigate(['/integration/aem/', this.integrationId, 'indexing-rule', 'list']);
      },
      response => {
        this.notifier.notify("error", "Indexing Rule was error: " + response);
      },
      () => {
        // The POST observable is now completed.
      });
  }
}
