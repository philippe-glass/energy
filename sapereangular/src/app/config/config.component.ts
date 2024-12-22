import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.scss']
})
export class ConfigComponent implements OnInit {
  nodeLocation = {"name":"", "host":"", "restPort":"",  "mainPort":""};
  nodeContext = {};
  neighbourNodes = "";
  allNodeLocations = [];
  neighboursUpdateRequest = {"neighbourNodes" : []};
  //test_list = [];
  nodeLocationToAdd = {"name":"", "host":"localhost", "restPort":"",  "mainPort":""};


  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {
    this.loadContent();
  }


 loadContent() {
  var serviceUrl = this._constant.baseAppUrl+'config/nodeContext';
  console.log("serviceUrl = ", serviceUrl);
  this.httpClient.get(serviceUrl).subscribe((res:any[]) => {
    this.nodeContext = res;
    this.nodeLocation = this.nodeContext["nodeLocation"];
    console.log("nodeContext = ", this.nodeContext, "nodeLocation");
    this.neighbourNodes = this.nodeContext["neighbourNodes"];
    console.log("this.nodeContext['neighbourNodes'] = ", this.nodeContext["neighbourNodes"]);
    this.neighboursUpdateRequest["neighbourNodes"] = this.nodeContext["neighbourNodes"];
    //this.test_list = this.nodeContext["neighbourNodes"];
    console.log("nodeContext = ", this.nodeContext, "neighbourNodes = ", this.neighbourNodes);
    console.log("neighboursUpdateRequest = ", this.neighboursUpdateRequest);
    this.nodeLocationToAdd = {"name":"", "host":"localhost", "restPort":"",  "mainPort":""};
  });


  this.httpClient.get(this._constant.baseAppUrl+'config/allNodeLocations').subscribe((res:any[]) => {
    this.allNodeLocations = res;
    console.log("allNodeLocations = ", this.allNodeLocations);
  });
 }


  ngOnInit() {
  }
  
  updateNeighbours() {
    console.log("updateNeighbours : neighboursUpdateRequest[neighbourNodes] = ", this.neighboursUpdateRequest["neighbourNodes"]);
    //console.log("updateNeighbours : test_list = ", this.test_list);
    //this.neighboursUpdateRequest["neighbourNodes"] = this.test_list;
    this.httpClient.post(this._constant.baseAppUrl+'config/updateNeighbours',
      this.neighboursUpdateRequest
      , { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
        this.loadContent();
      })
  }

  addConfig() {
    if(this.nodeLocationToAdd.name == '' ) {
      console.log("node name is empty");
    } else if (this.nodeLocationToAdd.host == '') {
      console.log("host is empty");
    } else if (this.nodeLocationToAdd.mainPort == '') {
      console.log("main port is empty");
    } else if (this.nodeLocationToAdd.restPort == '') {
      console.log("rest port is empty");
    } else {
      console.log("addConfig : nodeLocationToAdd = " , this.nodeLocationToAdd);
      var serviceUrl = this._constant.baseAppUrl+'config/addNodeLocation';
      console.log("addConfig : call service " ,serviceUrl);
      this.httpClient.post(serviceUrl,  this.nodeLocationToAdd , { responseType: 'text' }).
      subscribe(res => {
        console.log(res);
        this.loadContent();
      })
    }
  }
}
