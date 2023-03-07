import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-localservices',
  templateUrl: './localservices.component.html',
  styleUrls: ['./localservices.component.scss']
})
export class LocalservicesComponent implements OnInit {

  lsas = [];
  actions;
  states;
  show=false;
  selectedLsa="";
  text;
  constructor(private httpClient: HttpClient,private _constant: ConstantsService) { 
    this.httpClient.get(this._constant.baseAppUrl+'lsasList').
      subscribe((res :any[])=> {
        this.lsas=res;
      })

  }

  ngOnInit() {
  }

  onSelect(lsa){
    this.show=true;
    this.selectedLsa=lsa.name;
    this.httpClient.get(this._constant.baseAppUrl+'qtable/'+lsa.name).subscribe(
      res=>{
        this.actions=Object.values(res);
        this.states=Object.keys(res);
      }
    )
  }

  onclickQ(){
    this.show=false;
  }

  diffuse(){
    this.httpClient.get(this._constant.baseAppUrl+'diffuse?name=' + this.selectedLsa + '&hops=1', { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
      })

  }
}
