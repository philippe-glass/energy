import { Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';



@Injectable({
  providedIn: 'root'
})
export class ConstantsService {

  readonly baseAppUrl: string = environment['baseAppUrl'];

  //readonly baseAppUrl: string = '/app';

  constructor() {
    console.log("environment = ", environment);
  }
}
