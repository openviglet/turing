import { Component} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'sn-site-field-root-page',
    templateUrl: './sn-site-field-root-page.component.html',
    standalone: false
})
export class TurSNSiteFieldRootPageComponent {

  constructor(private route: ActivatedRoute) {
 }
}
