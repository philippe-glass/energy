import { Component, OnInit, Input } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-addservice',
  templateUrl: './addservice.component.html',
  styleUrls: ['./addservice.component.scss']
})
export class AddserviceComponent implements OnInit {

  name: "";
  input: "";
  output: "";
  number="";
  inputs="";
  outputs="";
  constructor(private httpClient: HttpClient,private _constant: ConstantsService) { }

  ngOnInit() {
  }

  addServices(){
    this.httpClient.post('/app/config/addServiceSim',
    { "number": this.number, "input":this.inputs.toString().split(','),"output":this.outputs.toString().split(',') }, { responseType: 'text' }).
    subscribe(res => {
      console.log(res);
    })
  }
  
  showtest() {

    this.httpClient.post(this._constant.baseAppUrl+'addService',
      { "name": this.name, "input": this.input.split(','), "output": this.output.split(',') }, { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
      })
  }

}
