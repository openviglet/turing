import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NotifierService} from 'angular-notifier-updated';
import {Observable} from 'rxjs';
import {TurIntegrationIndexingRule} from "../../../model/integration-indexing-rule.model";
import {TurIntegrationIndexingRuleService} from "../../../service/integration-indexing-rule.service";

@Component({
    selector: 'integration-indexing-rules-list-page',
    templateUrl: './integration-indexing-rules-list-page.component.html',
    standalone: false
})
export class TurIntegrationIndexingRulesListPageComponent {
  private integrationId: string;
  private turIntegrationIndexingRules: Observable<TurIntegrationIndexingRule[]>;
  filterText: string;

  constructor(
    private readonly notifier: NotifierService,
    private turIntegrationIndexingRuleService: TurIntegrationIndexingRuleService,
    private activatedRoute: ActivatedRoute) {
    this.integrationId = this.activatedRoute.parent?.parent?.snapshot.paramMap.get('id') || "";
    this.turIntegrationIndexingRuleService.setIntegrationId(this.integrationId);
    this.turIntegrationIndexingRules = turIntegrationIndexingRuleService.queryAll();
    this.filterText = "";
  }

  getIntegrationId(): string {
    return this.integrationId;
  }

  getTurIntegrationIndexingRules(): Observable<TurIntegrationIndexingRule[]> {
    return this.turIntegrationIndexingRules;
  }
}
