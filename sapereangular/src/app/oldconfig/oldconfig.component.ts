import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-config',
  templateUrl: './oldconfig.component.html',
  styleUrls: ['./oldconfig.component.scss']
})
export class OldConfigComponent implements OnInit {

  config = [];
  name="";
  localip="";
  localport="";
  neighbours="";

  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {
    this.httpClient.get(this._constant.baseAppUrl+'/oldconfig/').subscribe((res:any[]) => {
    this.config=res;
    this.name= this.config[0]["name"];
    this.localip = this.config[0]["localip"];
    this.localport = this.config[0]["localport"];
    this.neighbours = this.config[0]["neighbours"];
    this.update();
  })
  }

  ngOnInit() {
  }

  update(){
    this.httpClient.post(this._constant.baseAppUrl+'/oldconfig/update',
    { "name": this.name, "localip":this.localip, "localport":this.localport ,"neighbours":this.neighbours.toString().split(',') }, { responseType: 'text' }).
    subscribe(res => {
      console.log(res);
    })
  }

}
