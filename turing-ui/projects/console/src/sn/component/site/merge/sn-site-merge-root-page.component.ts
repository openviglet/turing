import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'sn-site-merge-root-page',
    templateUrl: './sn-site-merge-root-page.component.html',
    standalone: false
})
export class TurSNSiteMergeRootPageComponent {
  constructor(private route: ActivatedRoute) {
  }
}
