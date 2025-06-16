import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TurLoggingRootPageComponent} from "./component/root/logging-root-page.component";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {OcticonsModule} from "angular-octicons";
import {TurLoggingRoutingModule} from "./logging-routing.module";
import {TurCommonsModule} from "../commons/commons.module";
import {RouterModule} from "@angular/router";
import {MomentModule} from "ngx-moment";
import {TurLoggingIndexingPageComponent} from "./component/indexing/logging-indexing-page.component";
import {TurLoggingService} from "./service/logging.service";
import {TurLoggingServerPageComponent} from "./component/server/logging-server-page.component";
import {TurLoggingAemPageComponent} from "./component/aem/logging-aem-page.component";

@NgModule({
  declarations: [
    TurLoggingRootPageComponent,
    TurLoggingServerPageComponent,
    TurLoggingAemPageComponent,
    TurLoggingIndexingPageComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    OcticonsModule,
    TurLoggingRoutingModule,
    TurCommonsModule,
    RouterModule,
    MomentModule
  ],
  providers: [
    TurLoggingService
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class TurLoggingModule {
}
