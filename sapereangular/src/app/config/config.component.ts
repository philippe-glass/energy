import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.scss']
})
export class ConfigComponent implements OnInit {
  nodeConfig = {"name":"", "host":"", "restPort":"",  "mainPort":""};
  nodeContext = {};
  neighbourNames = "";
  allNodeConfigs = [];
  neighboursUpdateRequest = {"neighboursConfigId" : []};
  nodeConfigToAdd = {"name":"", "host":"localhost", "restPort":"",  "mainPort":""};


  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {
    this.loadContent();
  }


 loadContent() {
  var serviceUrl = this._constant.baseAppUrl+'config/nodeContext';
  console.log("serviceUrl = ", serviceUrl);
  this.httpClient.get(serviceUrl).subscribe((res:any[]) => {
    this.nodeContext = res;
    this.nodeConfig = this.nodeContext["nodeConfig"];
    //console.log("nodeContext = ", this.nodeContext, "nodeConfig");
    this.neighbourNames = this.nodeContext["neighbourNames"];
    console.log("neighbourNodeConfigIds = ", this.nodeContext["neighbourNodeConfigIds"]);
    this.neighboursUpdateRequest["neighboursConfigId"] = this.nodeContext["neighbourNodeConfigIds"];
    console.log("nodeContext = ", this.nodeContext, "neighbourNames = ", this.neighbourNames);
    this.nodeConfigToAdd = {"name":"", "host":"localhost", "restPort":"",  "mainPort":""};
  });


  this.httpClient.get(this._constant.baseAppUrl+'config/allNodeConfigs').subscribe((res:any[]) => {
    this.allNodeConfigs = res;
    //console.log("allNodeConfigs = ", this.allNodeConfigs);
  });
 }


  ngOnInit() {
  }
  
  updateNeighbours() {
    console.log("updateNeighbours : neighboursUpdateRequest = ", this.neighboursUpdateRequest);
    this.httpClient.post(this._constant.baseAppUrl+'config/updateNeighbours',
      this.neighboursUpdateRequest
      , { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
        this.loadContent();
      })
  }

  addConfig() {
    if(this.nodeConfigToAdd.name == '' ) {
      console.log("node name is empty");
    } else if (this.nodeConfigToAdd.host == '') {
      console.log("host is empty");
    } else if (this.nodeConfigToAdd.mainPort == '') {
      console.log("main port is empty");
    } else if (this.nodeConfigToAdd.restPort == '') {
      console.log("rest port is empty");
    } else {
      console.log("addConfig : nodeConfigToAdd = " , this.nodeConfigToAdd);
      var serviceUrl = this._constant.baseAppUrl+'config/addNodeConfig';
      console.log("addConfig : call service " ,serviceUrl);
      this.httpClient.post(serviceUrl,  this.nodeConfigToAdd , { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
        this.loadContent();
      })
    }
  }
}
