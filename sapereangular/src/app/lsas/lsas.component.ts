import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-lsas',
  templateUrl: './lsas.component.html',
  styleUrls: ['./lsas.component.scss']
})
export class LsasComponent implements OnInit {
  lsas = [];
  list_lsa = [];

  constructor(private httpClient: HttpClient,private _constant: ConstantsService) { 
    this.httpClient.get(this._constant.baseAppUrl+'lsasObj').
      subscribe((res :any[])=> {
        this.list_lsa=res;
        console.log(this._constant.baseAppUrl+'lsas2');
        console.log("this.httpClient.get : lsas2.lsas[0] = ", this.list_lsa[0]);
      })
  }

  ngOnInit() {
  }

}
