import { Component, OnInit } from '@angular/core';
import { Edge, Node } from '@swimlane/ngx-graph';
import * as shape from 'd3-shape';
import { nodes, links } from './data';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-graph',
  templateUrl: './graph.component.html',
  styleUrls: ['./graph.component.scss']
})
export class GraphComponent implements OnInit {

  lsas=[];
  node=[];
  
  constructor(private httpClient: HttpClient,private _constant: ConstantsService) { 
    this.httpClient.get(this._constant.baseAppUrl+'lsasList').
      subscribe((res :any[])=> {
        for (let index = 0; index < res.length; index++) {
          this.lsas.push({'id':res[index]['name'],'source':res[index]['input'][0],'target':res[index]['output'][0],'label':res[index]['name']})
        }
        this.lsas=res;
        console.log(this.lsas);
      })

      this.httpClient.get(this._constant.baseAppUrl+'node').
      subscribe((res :any[])=> {
        for (let index = 0; index < res.length; index++) {
          this.node.push({'id':res[index],'label':res[index]});
        }
        console.log(this.node);
      })

  }
  //nodes: Node[] = nodes;
  nodes: Node[] = this.node;
  //links: Edge[] = links;
  links: Edge[] = this.lsas;
  // line interpolation
  curveType: string = 'Bundle';
  curve: any = shape.curveLinear;
  interpolationTypes = [
    'Bundle',
    'Cardinal',
    'Catmull Rom',
    'Linear',
    'Monotone X',
    'Monotone Y',
    'Natural',
    'Step',
    'Step After',
    'Step Before'
  ];  

  draggingEnabled: boolean = true;
  panningEnabled: boolean = true;
  zoomEnabled: boolean = true;
  zoomSpeed: number = 0.1;
  minZoomLevel: number = 0.1;
  maxZoomLevel: number = 1.0;
  panOnZoom: boolean = true;
  autoZoom: boolean = false;
  autoCenter: boolean = true; 

  ngOnInit() {
    this.setInterpolationType(this.curveType);
  }
   
  setInterpolationType(curveType) {
    this.curveType = curveType;
    if (curveType === 'Bundle') {
      this.curve = shape.curveBundle.beta(1);
    }
    if (curveType === 'Cardinal') {
      this.curve = shape.curveCardinal;
    }
    if (curveType === 'Catmull Rom') {
      this.curve = shape.curveCatmullRom;
    }
    if (curveType === 'Linear') {
      this.curve = shape.curveLinear;
    }
    if (curveType === 'Monotone X') {
      this.curve = shape.curveMonotoneX;
    }
    if (curveType === 'Monotone Y') {
      this.curve = shape.curveMonotoneY;
    }
    if (curveType === 'Natural') {
      this.curve = shape.curveNatural;
    }
    if (curveType === 'Step') {
      this.curve = shape.curveStep;
    }
    if (curveType === 'Step After') {
      this.curve = shape.curveStepAfter;
    }
    if (curveType === 'Step Before') {
      this.curve = shape.curveStepBefore;
    }
  }

 

}
