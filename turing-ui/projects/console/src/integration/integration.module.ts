import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TurIntegrationInstanceService} from './service/integration-instance.service';
import {TurIntegrationInstanceListPageComponent} from './component/instance/integration-instance-list-page.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TurIntegrationRoutingModule} from './integration-routing.module';
import {TurCommonsModule} from '../commons/commons.module';
import {OcticonsModule} from 'angular-octicons';
import {RouterModule} from '@angular/router';
import {TurIntegrationRootPageComponent} from './component/root/integration-root-page.component';
import {TurIntegrationVendorService} from './service/integration-vendor.service';
import {TurLocaleService} from '../locale/service/locale.service';
import {
  TurIntegrationAemSourceComponent
} from "./component/instance/aem/integration-aem-source.component";
import {TurIntegrationAemSourceService} from "./service/integration-aem-source.service";
import {TurIntegrationAemListSourceComponent} from "./component/instance/aem/integration-aem-list-source.component";
import {
  TurIntegrationInstanceDetailPageComponent
} from "./component/instance/integration-instance-detail-page.component";
import {TurIntegrationAemMenuPageComponent} from "./component/instance/aem/integration-aem-menu-page.component";
import {TurIntegrationWcMenuPageComponent} from "./component/instance/wc/integration-wc-menu-page.component";
import {TurIntegrationWcPageComponent} from "./component/instance/wc/integration-wc-page.component";
import {TurIntegrationWcSourceService} from "./service/integration-wc-source.service";
import {TurIntegrationWcListPageComponent} from "./component/instance/wc/integration-wc-list-page.component";
import {
  TurIntegrationMonitoringPageComponent
} from "./component/instance/monitoring/integration-monitoring-page.component";
import {
  TurIntegrationIndexingRulesListPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-list-page.component";
import {
  TurIntegrationIndexingRulesPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-page.component";
import {
  TurSNIndexingRulesRootPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-root-page.component";
import {MomentModule} from "ngx-moment";
import {TurIntegrationIndexingRuleService} from "./service/integration-indexing-rule.service";
import {TurIntegrationMonitoringService} from "./service/integration-monitoring.service";

@NgModule({
  declarations: [
    TurIntegrationRootPageComponent,
    TurIntegrationInstanceDetailPageComponent,
    TurIntegrationInstanceListPageComponent,
    TurIntegrationAemSourceComponent,
    TurIntegrationAemListSourceComponent,
    TurIntegrationAemMenuPageComponent,
    TurIntegrationWcMenuPageComponent,
    TurIntegrationWcPageComponent,
    TurIntegrationWcListPageComponent,
    TurIntegrationMonitoringPageComponent,
    TurIntegrationIndexingRulesListPageComponent,
    TurIntegrationIndexingRulesPageComponent,
    TurSNIndexingRulesRootPageComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    OcticonsModule,
    TurIntegrationRoutingModule,
    TurCommonsModule,
    RouterModule,
    MomentModule,
  ],
  providers: [
    TurIntegrationInstanceService,
    TurIntegrationVendorService,
    TurLocaleService,
    TurIntegrationAemSourceService,
    TurIntegrationWcSourceService,
    TurIntegrationIndexingRuleService,
    TurIntegrationMonitoringService
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class TurIntegrationModule {
}
