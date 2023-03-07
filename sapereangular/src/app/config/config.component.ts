import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.scss']
})
export class ConfigComponent implements OnInit {

  config = [];
  name="";
  localip="";
  localport="";
  neighbours="";

  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {
    this.httpClient.get(this._constant.baseAppUrl+'/config/').subscribe((res:any[]) => {
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
    this.httpClient.post(this._constant.baseAppUrl+'/config/update',
    { "name": this.name, "localip":this.localip, "localport":this.localport ,"neighbours":this.neighbours.toString().split(',') }, { responseType: 'text' }).
    subscribe(res => {
      console.log(res);
    })
  }

}
