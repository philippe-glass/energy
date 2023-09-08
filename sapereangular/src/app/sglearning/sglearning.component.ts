import { Component, OnInit, OnChanges, ChangeDetectorRef, ViewChild, ElementRef, Input, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders  } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { fnum_minus_plus, fnum0, fnum2,fnum3, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay } from '../common/util.js';
@Component({
  selector: 'app-sglearning',
  templateUrl: './sglearning.component.html',
  styleUrls: ['./sglearning.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class SGLearningComponent implements OnInit  {
  private margin: any = { top: 50, bottom: 50, left: 50, right: 20};

  private listNodeTransitionMatrices = null;
  private mapNodeTransitionMatrices = {};
  private nodeTransitionMatrices = null;
  private variables = [];
  private all_variables = [];
  private listTimeWindows = [];
  private listStates = [];
  private stateNb = 0;
  private sTimeWindow = "";
  private prediction = {};
  private fedAvgResult = {};
  private listPredictions = [];
 // private avgSateProbabilities = [];
  private listCorrections = [];
  private listPredictionItems = [];
  private listPredictionsTotal = {};
  private listStatisitcs = [];
  private totalStatistics = {};
  private listErrors = [];
  private entropieResult = {};
  //private nbPredictionItems = 0;
  private changeDetectorRef: ChangeDetectorRef;
  private listStateDates = [];
  private defaultNodeLocation = {};
  private mapAllNodeLocations = {};
  private listYesNo = [{"label":"no", "value":false}, {"label":"yes", "value":true} ];

  // form for Red-avg checkuo
  private fedAvgCheckupRequest = {
    "variableName":""
  }

  // form for statistic request
  private statisticsRequest = {
     "minComputeDay":getDefaultTargetDay()
    ,"maxComputeDay":getDefaultTargetDay()
    ,"minTargetDay":null
    ,"maxTargetDay":null
    ,"nodeName":""
    ,"nodeLocation":{}
    ,"mergeHorizons":true
    ,"mergeUseCorrections":false
    ,"mergableFields":[
          {"value":"horizon", "label":"horizon"}
        , {"value":"useCorrection","label":"use correction"}
        , {"value":"hour", "label":"time slot"}]
    ,"fieldsToMerge":["horizon"]
  };
  // form for single prediction request
  private singlePredictionRequest = {
       "initDay":getDefaulInitDay()
      ,"initTime":getDefaultInitTime()
      //,"initDate":null
      ,"targetDay":getDefaultTargetDay()
      ,"targetTime": getDefaultTargetTime()
	   // ,"targetDate":null
     // ,"nodeLocation":{}
      ,"nodeName":""
	    ,"useCorrections":false
  }
  // form for massive prediction request
  private massivePredictionRequest = {
      "targetDay":getDefaultTargetDay()
     ,"targetHour":Math.max(0,getDefaultHour()-2)
     ,"horizonInMinutes":5
     ,"nodeLocation":{}
     ,"nodeName":""
     ,"variableName":""
     ,"useCorrections":true
     ,"generateCorrections":false
  };
  // filter for matrix refresh
  private matrixFilter = {
       "nodeLocation":{}
      ,"nodeName":""
      ,"variableName":""
      ,"startHourMin":getDefaultHour()
      ,"startHourMax":(1+getDefaultHour())
      ,"includeCorrections":true
  }
  private displayDistanceVector = false; //"display_none"; // display_yes

  constructor(private httpClient: HttpClient,private _constant: ConstantsService, public _chartElem: ElementRef, private cd: ChangeDetectorRef) {
    this.changeDetectorRef = cd;
    //this.changeDetectorRef.detectChanges();
    this.httpClient.get(this._constant.baseAppUrl+'energy/getNodeConfig').
      subscribe((result :any[])=> {
        this.defaultNodeLocation = result;
        console.log("this.defaultNodeLocation = ", this.defaultNodeLocation);
        var defaultNodeName = this.defaultNodeLocation["name"];
          console.log("defaultNodeName", defaultNodeName);
          if(this.statisticsRequest['nodeName'] == "") {
            this.statisticsRequest['nodeName'] = defaultNodeName;
            this.statisticsRequest['nodeLocation'] = this.defaultNodeLocation;;
          }
          if(this.singlePredictionRequest['nodeName'] == "") {
            this.singlePredictionRequest['nodeName'] = defaultNodeName;
           // this.singlePredictionRequest['nodeLocation'] = this.defaultNodeLocation;;
          }
          if(this.massivePredictionRequest['nodeName'] == "")  {
            this.massivePredictionRequest['nodeName'] = defaultNodeName;
            //this.massivePredictionRequest['nodeLocation'] = this.defaultNodeLocation;;
          }
          if(this.matrixFilter['nodeName'] == "")  {
            //this.matrixFilter['location'] = defaultLocation;
            this.matrixFilter['nodeName'] = defaultNodeName;
            //this.matrixFilter['nodeLocation'] = this.defaultNodeLocation;
          }
      });

    this.httpClient.get(this._constant.baseAppUrl+'energy/getMapAllNodeConfigs').
      subscribe((result :any[])=> {
        this.mapAllNodeLocations = result;
        Object.entries(this.mapAllNodeLocations).forEach(([key, value]) => {
          console.log('------- getMapAllNodeConfigs Key : ' + key + ', Value : ' + value["mainServiceAddress"], value["url"])
        })
      });

      this.httpClient.get(this._constant.baseAppUrl+'energy/getStateDates').
      subscribe((result :any[])=> {
        this.listStateDates = result;
        console.log("getStateDates listStateDates :" , this.listStateDates);
      });
    this.refreshMatrices();
   }

   computeRowSum(row) {
    var sum=0;
    for(var idx in row) {
      sum+=row[idx];
    }
    return sum;
  }

  selectTransitionMatrix(variableName, timeWindow) {
    var subMap = this.mapNodeTransitionMatrices[timeWindow];
    if(subMap === undefined) {
      return undefined;
    }
    return subMap.mapMatrices[variableName];
  }



  aux_selectNormalizedMatrixRow(trMatrix, rowId, includeCorrections) {
    if (trMatrix === undefined) {
      return [];
    }
    var norm_matrix = includeCorrections ? trMatrix["normalizedMatrix2"] :  trMatrix["normalizedMatrix1"];
    return norm_matrix.array[rowId];
  }

  aux_selectAllObsMatrixRow(trMatrix, rowId) {
    if (trMatrix === undefined) {
      return [];
    }
    var matrix1 = trMatrix.allObsMatrix;
    return matrix1.array[rowId];
  }

  aux_selectAllCorrectionsMatrixRow(trMatrix, rowId) {
    if (trMatrix === undefined) {
      return [];
    }
    var matrix1 = trMatrix.allCorrectionsMatrix;
    return matrix1.array[rowId];
  }

  aux_displayAllObsMatrixRowSum(trMatrix, rowId) {
    var row = this.aux_selectAllObsMatrixRow(trMatrix, rowId);
    var obsNb = this.computeRowSum(row);
    var result ="" + obsNb;
  return result;
  }

  aux_displayAllCorrectionsMatrixRowSum(trMatrix, rowId){
    var row = this.aux_selectAllCorrectionsMatrixRow(trMatrix, rowId);
    var obsNb = this.computeRowSum(row);
    var result = this.displayCorrectionsNb(obsNb);
  return result;
  }

  aux_selectAllObsMatrixCell(trMatrix, rowId, columnId) {
    //console.log("selectAllObsMatrixCell begin", variableName, timeWindow, rowId);
    if (trMatrix === undefined) {
      return null;
    }
    var matrix = trMatrix.allObsMatrix;
    return matrix.array[rowId][columnId];
  }

  aux_selectAllCorrectionsMatrixCell(trMatrix, rowId, columnId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    if (trMatrix === undefined) {
      return null;
    }
    var matrix = trMatrix.allCorrectionsMatrix;
    return matrix.array[rowId][columnId];
  }

  aux_getClassNormalizedMatrixRow(trMatrix, rowId) {
    var row =  this.aux_selectNormalizedMatrixRow(trMatrix, rowId, false);
    var sum = this.computeRowSum(row);
    if(sum==0) {
      return "warning_high";
    }
    return "";
  }

  selectNormalizedMatrixRow2(variableName, timeWindow, rowId) {
    var includeCorrections = this.doIncludeCorrections();
    //console.log("selectNormalizedMatrixRow2 variableName = ",variableName, " , includeCorrections = ", includeCorrections);
    return this.selectNormalizedMatrixRow(variableName, timeWindow, rowId, includeCorrections);
  }

  selectNormalizedMatrixRow(variableName, timeWindow, rowId, includeCorrections) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_selectNormalizedMatrixRow(trMatrix, rowId, includeCorrections);
  }

  selectAllObsMatrixRow(variableName, timeWindow, rowId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_selectAllObsMatrixRow(trMatrix, rowId);
  }

 displayAllObsMatrixRowSum(variableName, timeWindow, rowId) {
  var obsNb = this.computeRowSum(this.selectAllObsMatrixRow(variableName, timeWindow, rowId));
  var result ="" + obsNb;
  return result;
 }

 doIncludeCorrections() {
  var includeCorrections = this.matrixFilter['includeCorrections'];
  var sIncludeCorrections = includeCorrections.toString();
  return sIncludeCorrections=='true';
 }

 displayAllCorrectionsMatrixRowSum(variableName, timeWindow, rowId) {
  var includeCorrections = this.matrixFilter['includeCorrections'];
  var sIncludeCorrections = includeCorrections.toString();
  //console.log("displayAllCorrectionsMatrixRowSum inclue correction = ", this.filter_includeCorrections, typeof this.filter_includeCorrections, sIncludeCorrections );
  if(this.doIncludeCorrections()) {
    var correctionsNb = this.computeRowSum(this.selectAllCorrectionsMatrixRow(variableName, timeWindow, rowId));
    return this.displayCorrectionsNb(correctionsNb);
  }
 }

  selectAllCorrectionsMatrixRow(variableName, timeWindow, rowId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_selectAllCorrectionsMatrixRow(trMatrix, rowId);
  }

  displayAllObsMatrixCell(variableName, timeWindow, rowId, columnId) {
    return "" + this.selectAllObsMatrixCell(variableName, timeWindow, rowId, columnId);
  }

  displayAllCorrectionsMatrixCell(variableName, timeWindow, rowId, columnId) {
    if(this.doIncludeCorrections()) {
      var correctionsNb = this.selectAllCorrectionsMatrixCell(variableName, timeWindow, rowId, columnId);
      return this.displayCorrectionsNb(correctionsNb)
    }
    return "";
  }

  displayCorrectionsNb(correctionsNb) {
    var result ="";
    if(correctionsNb > 0) {
      result = result + " +" +  correctionsNb;
    } else if(correctionsNb < 0) {
      result = result + " " +  correctionsNb;
    }
    return result;
  }

  selectAllObsMatrixCell(variableName, timeWindow, rowId, columnId) {
    //console.log("selectAllObsMatrixCell begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_selectAllObsMatrixCell(trMatrix, rowId, columnId);
  }

  selectAllCorrectionsMatrixCell(variableName, timeWindow, rowId, columnId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_selectAllCorrectionsMatrixCell(trMatrix, rowId, columnId);
  }

  getClassDifferentialItem(value) {
    var value2 = Math.abs(value);
    if(value2 < 0.01) {
      return "differential_low";
    } else if(value2 < 0.05) {
      return "differential_medium";
    } else if(value2 < 0.1) {
      return "";
    } else {
      return "differential_high";
    }
    return "";
  }

  getClassDisplayDistanceVector() {
    if(this.displayDistanceVector) {
      return 'display_yes';
    } else {
      return 'display_none';
    }
  }

  getClassNormalizedMatrixRow(variableName, timeWindow, rowId) {

    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    return this.aux_getClassNormalizedMatrixRow(trMatrix, rowId);
  }

  geClassMostLikelyState(predictionResult) {
    if(predictionResult['mostLikelyStateOK']) {
      return "warning_ok";
    } else if (predictionResult['actualTargetState'] != null) {
      return "warning_high";
    } else {
      console.log("geClassMostLikelyState", predictionResult);
    }
  }


  getClassLineResult(object) {
    //console.log("getClassLineResult", object['firstOfRowBloc']);
    if(object['firstOfRowBloc']) {
      return "next_bloc3";
    }
    return "";
  }

  getClassState(state) {
    //this.stateNb = this.listStates.length;
    if( this.stateNb > 0) {
      var stateId = state['id']; // predictionResult['actualTargetState']['id'];
      var pDec1 = 1+Math.floor(10*stateId/ this.stateNb);
      var result = "state_"+pDec1;
      //console.log("getClassActualState", stateId, this.stateNb, pDec1, result);
      return result
    }
    return "";
  }

  geClassEntropie(statisticItem) {
    if(statisticItem == null || statisticItem === undefined) {
      return "";
    }
    var entropie = statisticItem['shannonEntropie'];
    //console.log("geClassEntropie", entropie);
    if(1*entropie>=0.999) {
      return "rate_very_poor";
    } else if (1*entropie>=0.5) {
      return "rate_poor";
    } else if (1*entropie>=0.25) {
      return "rate_fairly_good";
    } else if (1*entropie > 0) {
      return "";
    }
    return "";
  }

  geClassSucessRate(rate) {
    if(rate == null) {
      return "";
    }
    if(1*rate >= 0.999) {
      return "rate_perfect";
    } else if(1*rate >= 0.95) {
      return "rate_very_good";
    } else if (1*rate>=0.90) {
      return "rate_good";
    } else if (1*rate>=0.80) {
      return "rate_fairly_good";
    } else if (1*rate>=0.70) {
      return "rate_medium";
    } else if (1*rate>= 0.50) {
      return "rate_poor";
    } else if (1*rate>= 0.25) {
      return "rate_very_poor";
    } else {
      return "rate_disastrous";
    }
  }

  getClassProba(pValue, rowIdx, colIdx){
    if(pValue>0) {
      if(rowIdx>=0 && rowIdx==colIdx && pValue>0.999) {
        return "p_stationary";
      }
      var val2 = 10*pValue;
      var pDec1 = Math.floor(10*pValue);
      //console.log("greyLevel", pValue,val2, pDec1);
      return "p_"+pDec1;
    }
    return "";
  }
  getTransitionMatrixId(variableName, timeWindow) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return 0;
    }
    //console.log("getTransitionMatrixId : ", trMatrix.key);
    return trMatrix.key.id;
  }

  getNbOfObservations(variableName, timeWindow) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return 0;
    }
    return trMatrix.nbOfObservations;
  }

 getNbOfCorrections(variableName, timeWindow) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return 0;
    }
    //console.log("getNbOfCorrections", trMatrix);
    return trMatrix.nbOfCorrections;
  }

  getNbOfIterations(variableName, timeWindow) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return 0;
    }
    return trMatrix.nbOfIterations;
  }

  refreshMatrices(){
    var matrixFilter = this.matrixFilter;
    console.log("refreshMatrices : matrixFilter = ", this.matrixFilter);
    var chosenNodeLocation = this.defaultNodeLocation;
    var chosenNodeName = matrixFilter["nodeName"];
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    if(chosenNodeName  != "" && this.mapAllNodeLocations.hasOwnProperty(chosenNodeName)) {
      console.log("refreshMatrices : chosenNodeName = ", chosenNodeName);
      console.log("refreshMatrices : mapAllNodeLocations = ", this.mapAllNodeLocations);
      chosenNodeLocation = this.mapAllNodeLocations[chosenNodeName];
      //matrixFilter["nodeLocation"] = chosenNodeLocation;
      matrixFilter["nodeName"] = chosenNodeLocation["nodeName"];
      console.log("refreshMatrices : chosenNodeLocation = ", chosenNodeLocation);
      chosenBaseUrl = chosenNodeLocation["url"];
    } else {
      console.log("init condition : defaultNodeConfig = ",  this.defaultNodeLocation);
    }
    console.log("refreshMatrices : matrixFilter = ", this.matrixFilter
    , "chosenNodeLocation = ", chosenNodeLocation, "chosenBaseUrl = ", chosenBaseUrl);
    var sep = chosenBaseUrl.endsWith("/")? "" : "/";
    var serviceUrl = chosenBaseUrl + "/" + 'allNodeTransitionMatrices';
    console.log("refreshMatrices : calling service 1", serviceUrl, chosenBaseUrl);
    //serviceUrl = serviceUrl.replace("//allNodeTransitionMatrices", "/allNodeTransitionMatrices");
    //serviceUrl = "http://localhost:9191/energy/allNodeTransitionMatrices";
    console.log("refreshMatrices : calling service 2", serviceUrl);
    let str = ""+this.matrixFilter;

    //this.httpClient.post(this._constant.baseAppUrl+'energy/allNodeTransitionMatrices', this.matrixFilter , { responseType: 'json' }).
    let apiHeader = new HttpHeaders().set('Content-Type', 'application/json; charset=utf-8');
    //let bytes = str.getBytes();
    apiHeader.set("charset", "utf-8");
    apiHeader.set( 'Authorization', 'Bearer my-token');
    //apiHeader.set("Content-Length", "utf-8");


    let filterParams = new HttpParams();
    Object.entries(this.matrixFilter).forEach(([key, value]) => {
        filterParams = filterParams.append(key,""+value);
    });
    console.log("refreshMatrices filterParams = ", filterParams);
    this.httpClient.get(serviceUrl,  { params: filterParams } ).
    //this.httpClient.post(serviceUrl, this.matrixFilter , { responseType: 'json' , headers:apiHeader}).
      subscribe((res :any[])=> {
        console.log("subscribe result = ", res);
        this.listNodeTransitionMatrices =  res;
        this.nodeTransitionMatrices=null
        this.listTimeWindows = [];
        this.listStates = [];
        //this.changeDetectorRef = cd;
        this.changeDetectorRef.detectChanges();
        console.log("change detector", this.changeDetectorRef);
        console.log(this.listNodeTransitionMatrices);
        for(var idx in this.listNodeTransitionMatrices) {
            var nodeTransitionMatrices = this.listNodeTransitionMatrices[idx];
            if(this.nodeTransitionMatrices==null) {
              this.nodeTransitionMatrices = nodeTransitionMatrices;
              console.log("homeTransitionMatrices", this.nodeTransitionMatrices);
              this.variables = nodeTransitionMatrices.variables;
              if(this.all_variables == null || this.all_variables.length == 0) {
                this.all_variables = nodeTransitionMatrices.variables;
                console.log("all_variables = ", this.all_variables)
              }
              console.log("homeTransitionMatrices.statesList", nodeTransitionMatrices.statesList);
              for(var idxState in nodeTransitionMatrices.statesList) {
                var state = nodeTransitionMatrices.statesList[idxState];
                this.listStates.push(state);
              }
            }
            var sTimeWindow = formatTimeWindow(nodeTransitionMatrices);
            this.sTimeWindow = sTimeWindow;
            this.listTimeWindows.push(sTimeWindow);
            this.mapNodeTransitionMatrices[sTimeWindow] = nodeTransitionMatrices;
            //console.log("nodeTransitionMatrices = ", nodeTransitionMatrices);
            /*
            for (const nodeTransitionMatrices in this.nodeTransitionMatrices.mapMatrices) {
              //console.log("nodeTransitionMatrices", this.nodeTransitionMatrices);
              for (const variableName in this.nodeTransitionMatrices.mapMatrices) {
                //console.log("variableName",variableName, "matrix", this.nodeTransitionMatrices.mapMatrices[variableName]);
              }
            }*/
        }
        this.stateNb = this.listStates.length;
        console.log("variables", this.variables);
        console.log("listStates", this.listStates, this.stateNb);
        console.log("listTimeWindows", this.listTimeWindows);
        console.log("mapHomeTransitionMatrices", this.mapNodeTransitionMatrices);
        var test = this.getNbOfIterations("consumed", "09:00-10:00");
        console.log("getNbOfIterations", test);
        var test2 = this.selectNormalizedMatrixRow("consumed", "09:00-10:00", 1, false);
        console.log("selectNormalizedMatrixRow", test2, false);
        this.changeDetectorRef.detectChanges();
      });
  }

  getSinglePrediction(){
    var initDate = timeYYYYMMDDHMtoDate(this.singlePredictionRequest['initDay'], this.singlePredictionRequest['initTime']);
    var targetDate = timeYYYYMMDDHMtoDate(this.singlePredictionRequest['targetDay'], this.singlePredictionRequest['targetTime']);
    /*
    this.singlePredictionRequest['initDate'] = initDate;
    this.singlePredictionRequest['initDate'] = this.singlePredictionRequest['initDay'] + "T"+ this.singlePredictionRequest['initTime'];
    this.singlePredictionRequest['initDate'] = "2023-05-01";
    */
    console.log(initDate, initDate.getTime());
    /*
    this.singlePredictionRequest['targetDate'] = targetDate;
    this.singlePredictionRequest['targetDate'] = "2023-05-02";
    */
    this.singlePredictionRequest['longInitDate'] = initDate.getTime();
    this.singlePredictionRequest['longTargetDate'] = targetDate.getTime();
    console.log("getSinglePrediction singlePredictionRequest2 = ", this.singlePredictionRequest);
    var chosenNodeName =  this.singlePredictionRequest["nodeName"];
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    if(chosenNodeName  != "" && this.mapAllNodeLocations.hasOwnProperty(chosenNodeName)) {
      console.log("getSinglePrediction : chosenNodeName = ", chosenNodeName);
      var chosenNodeLocation = this.mapAllNodeLocations[chosenNodeName];
      this.singlePredictionRequest["nodeLocation"] = chosenNodeLocation;
      console.log("getSinglePrediction : chosenNodeLocation = ", chosenNodeLocation);
      chosenBaseUrl = chosenNodeLocation["url"];
    } else {
      this.singlePredictionRequest["nodeLocation"] = this.defaultNodeLocation;
    }
        var serviceUrl = chosenBaseUrl+'/getPrediction';
    console.log("getSinglePrediction : calling service", serviceUrl);
    let reqparams = new HttpParams();
    Object.entries(this.singlePredictionRequest).forEach(([key, value]) => {
      if(key != "_initTime" && key != "_targetTime" && key != "_initDay"
       && key != "_targetDay" && key != "_nodeLocation") {
        reqparams = reqparams.append(key,""+value);
      }
    });
    //this.httpClient.get(serviceUrl, this.singlePredictionRequest, { responseType: 'json' }).
    this.httpClient.get(serviceUrl,  { params: reqparams } ).
    subscribe(res => {
      this.prediction = res;
      console.log("getSinglePrediction : this.prediction = ", this.prediction);
      console.log("before changeDetectorRef");
      //this.reload();
      if(this.prediction.hasOwnProperty('mapLastResults')) {
        var mapLastResults = this.prediction['mapLastResults'];
        console.log("getSinglePrediction : mapLastResults", mapLastResults);
      }
      this.changeDetectorRef.detectChanges();

    })
  }

  getBooleanValue(aValue) {
    var result = null;
    if(aValue == 'false') {
      result = "false";
    }
    if(aValue == 'true') {
      result = "true";
    }
    console.log("getBooleanValue result = ", result);
    return result;
  }

  checkupFedAVG() {
    console.log("checkupFedAVG : fedAvgCheckupRequest = ", this.fedAvgCheckupRequest);
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    var serviceUrl = chosenBaseUrl+'/checkupFedAVG';
    console.log("checkupFedAVG : calling service", serviceUrl);
    let requestParams = new HttpParams();
    Object.entries(this.fedAvgCheckupRequest).forEach(([key, value]) => {
        if(value!= undefined )    {
          requestParams = requestParams.append(key,""+value);
        }
    });
    this.httpClient.get(serviceUrl, { params: requestParams }).
      subscribe((result :any[])=> {
        this.fedAvgResult = result;
        console.log("checkupFedAVG : fedAvgResult = ", this.fedAvgResult);
        var maptrMatrices = this.fedAvgResult['nodeTransitionMatrices']
        var nodeName = this.defaultNodeLocation["name"];
        console.log("nodeName = ", nodeName);
        var trMatrix = maptrMatrices[nodeName];
        console.log("trMatrix :", trMatrix);
        var idxState = 0;
        var row = this.aux_selectNormalizedMatrixRow(trMatrix, idxState, true);
        console.log("first row :", row);
      })
  }

  getStatistics() {
    console.log("getStatistics : statisticsRequest = ", this.statisticsRequest);
    var useCorrectionsFilter = this.statisticsRequest['useCorrectionsFilter']
    console.log("getStatistics1 : useCorrectionsFilter = ", this.statisticsRequest['useCorrectionsFilter'], typeof this.statisticsRequest['useCorrectionsFilter']);
    //this.statisticsRequest['useCorrectionsFilter'] = this.getBooleanValue(useCorrectionsFilter);
    this.statisticsRequest["useCorrectionFilter"] = null;
    if(useCorrectionsFilter == 'false') {
      this.statisticsRequest["useCorrectionFilter"] = "false";
    }
    if(useCorrectionsFilter == 'true') {
      this.statisticsRequest["useCorrectionFilter"] = "true";
    }
    var minComputeDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["minComputeDay"], "00:00");
    var maxComputeDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["maxComputeDay"], "00:00");
    //var minTargetDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["minTargetDay"], "00:00");
    //var maxTargetDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["maxTargetDay"], "00:00");
    console.log("getStatistics1 : minComputeDay = ", minComputeDay, " maxComputeDay = ", maxComputeDay);
    this.statisticsRequest["longMinComputeDay"] = minComputeDay.getTime();
    this.statisticsRequest["longMaxComputeDay"] = maxComputeDay.getTime();
    //this.statisticsRequest["longMinTargetDay"] = minTargetDay.getTime();
    //this.statisticsRequest["longMinTargetDay"] = maxTargetDay.getTime();
    console.log("getStatistics1 : useCorrectionsFilter = ", this.statisticsRequest['useCorrectionsFilter'], typeof this.statisticsRequest['useCorrectionsFilter']);
    var chosenNodeName =  this.statisticsRequest["nodeName"];
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    if(chosenNodeName  != "" && this.mapAllNodeLocations.hasOwnProperty(chosenNodeName)) {
      console.log("getStatistics1 : chosenNodeName = ", chosenNodeName);
      var chosenNodeLocation = this.mapAllNodeLocations[chosenNodeName];
      this.statisticsRequest["nodeLocation"] = chosenNodeLocation;
      console.log("getStatistics1 : chosenNodeLocation = ", chosenNodeLocation);
      chosenBaseUrl = chosenNodeLocation["url"];
    } else {
      this.statisticsRequest["nodeLocation"] = this.defaultNodeLocation;
    }

    var serviceUrl = chosenBaseUrl+'/computePredictionStatistics';
    console.log("getStatistics1 : calling service", serviceUrl, "generateCorrections = ", this.statisticsRequest["enerateCorrections"]);

    let requestParams = new HttpParams();
    Object.entries(this.statisticsRequest).forEach(([key, value]) => {
        if(key != "nodeLocation" && value!= undefined )    {
          requestParams = requestParams.append(key,""+value);
        }
    });
    console.log("chosenBaseUrl requestParams = ", requestParams);
    this.httpClient.get(serviceUrl,  { params: requestParams } ).
    subscribe((result :any[])=> {
          this.listStatisitcs = result;
          //this.listErrors = result["errors"];
          console.log("getStatistics : this.listStatisitcs = ", this.listStatisitcs, "listErrors = ", this.listErrors);
          console.log("before changeDetectorRef");
          console.log("fieldsToMerge", this.statisticsRequest['fieldsToMerge'])
          var weightedEntropie = 0.0;
          var weightedGiniIdx = 0.0;
          var weightedDifferential = 0.0;
          this.totalStatistics = {'nbOfPredictions':0, 'nbOfPredictions2':0, 'nbOfSuccesses':0, 'nb':0, 'arrayMeansOfProba':[], 'arraySumsOfProba':[]};
          for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
            this.totalStatistics['arrayMeansOfProba'][state_idx] = 0;
            this.totalStatistics['arraySumsOfProba'][state_idx] = 0;
          }
          var lastBeginHours = null;
          console.log("this.listStatisitcs.length = ", this.listStatisitcs.length);
          for(var i = 0; i < this.listStatisitcs.length; i++) {
            var nextStatistic = this.listStatisitcs[i];
            console.log("nextStatistic = ", nextStatistic);
            nextStatistic['firstOfRowBloc'] = false;
            var beginDate =  nextStatistic['timeSlot']['beginDate'];
            console.log("beginDate = ", beginDate);
            var datatimeSep = "T";
            if (!beginDate.includes("T")) {
              datatimeSep = " ";
            }
            var beginTime = (beginDate.split(datatimeSep))[1];
            var beginHours = beginTime.substring(0,2);
            //console.log("nextStatistic", beginDate, beginHours, lastBeginHours);
            if(lastBeginHours == null || (lastBeginHours != beginHours)) {
              nextStatistic['firstOfRowBloc'] = true;
            }
            lastBeginHours = beginHours;
            //console.log("nextStatistic = ", nextStatistic);
            this.totalStatistics['nbOfPredictions'] =  this.totalStatistics['nbOfPredictions'] + nextStatistic['nbOfPredictions'];
            this.totalStatistics['nbOfSuccesses'] =  this.totalStatistics['nbOfSuccesses'] + nextStatistic['nbOfSuccesses'];
            this.totalStatistics['nb'] =  1 + this.totalStatistics['nb'];
            if(nextStatistic['statesStatistic'] != null) {
              weightedEntropie+= nextStatistic['statesStatistic']['shannonEntropie'] *  nextStatistic['nbOfPredictions'];
              weightedGiniIdx+= nextStatistic['statesStatistic']['giniIndex'] *  nextStatistic['nbOfPredictions'];
            }
            //var arrayMeansOfProba = nextStatistic['arrayMeansOfProba'];
            //console.log("arrayMeansOfProba = ", arrayMeansOfProba);
            for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
              var toAdd =  nextStatistic['nbOfPredictions'] * nextStatistic['arrayMeansOfProba'][state_idx];
              this.totalStatistics['arraySumsOfProba'][state_idx] = this.totalStatistics['arraySumsOfProba'][state_idx] + toAdd ;
            }
            //console.log("arraySumsOfProba = ", this.totalStatistics['arraySumsOfProba']);
            if(nextStatistic['differential'] != null) {
              weightedDifferential+= nextStatistic['differential'] *  nextStatistic['nbOfPredictions'];
              this.totalStatistics['nbOfPredictions2'] =  this.totalStatistics['nbOfPredictions2'] + nextStatistic['nbOfPredictions'];
            }
          }
          console.log("totalStatistics = ", this.totalStatistics);
          this.totalStatistics['sucessRate'] = 0;
          this.totalStatistics['shannonEntropie'] = 0.0;
          this.totalStatistics['giniIndex'] = 0.0;
          this.totalStatistics['differential'] = 0.0;
          if(this.totalStatistics['nbOfPredictions'] > 0 ) {
            var nbOfPredictions = this.totalStatistics['nbOfPredictions'];
            this.totalStatistics['sucessRate'] = this.totalStatistics['nbOfSuccesses'] / nbOfPredictions;
            this.totalStatistics['shannonEntropie'] = weightedEntropie /  nbOfPredictions;
            this.totalStatistics['giniIndex'] = weightedGiniIdx / nbOfPredictions;
            for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
              this.totalStatistics['arrayMeansOfProba'][state_idx] = this.totalStatistics['arraySumsOfProba'][state_idx] / nbOfPredictions;
            }
            var nbOfPredictions2 = this.totalStatistics['nbOfPredictions2'];
            this.totalStatistics['differential'] = weightedDifferential / nbOfPredictions2;
          }
          console.log(" totalStatistics = ", this.totalStatistics);
          //console.log("arraySumsOfProba = ", this.totalStatistics['arraySumsOfProba']);
          this.changeDetectorRef.detectChanges();
        })
  }


  getMassivePredictions() {
    console.log("getMassivePredictions : massivePredictionRequest = " , this.massivePredictionRequest);
    var chosenNodeName =  this.massivePredictionRequest["nodeName"];
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    if(chosenNodeName  != "" && this.mapAllNodeLocations.hasOwnProperty(chosenNodeName)) {
      console.log("getMassivePredictions : chosenNodeName = ", chosenNodeName);
      var chosenNodeLocation = this.mapAllNodeLocations[chosenNodeName];
      this.massivePredictionRequest["nodeLocation"] = chosenNodeLocation;
      console.log("getMassivePredictions : chosenNodeLocation = ", chosenNodeLocation);
      chosenBaseUrl = chosenNodeLocation["url"];
    } else {
      this.massivePredictionRequest["nodeLocation"] = this.defaultNodeLocation;
    }
    var targetDay =  timeYYYYMMDDHMtoDate(this.massivePredictionRequest["targetDay"], "00:00");
    //var longTargetDay = targetDay.getTime();
    this.massivePredictionRequest["longTargetDay"]  = targetDay.getTime();

    let requestParams = new HttpParams();
    Object.entries(this.massivePredictionRequest).forEach(([key, value]) => {
      if(value!= undefined && key != "nodeLocation" ){
        requestParams = requestParams.append(key,""+value);
      }
    });
    var serviceUrl = chosenBaseUrl+'/getMassivePredictions';
    this.httpClient.get(serviceUrl, { params: requestParams }).
    subscribe((result :any[])=> {
          this.listPredictionItems = [];
          this.listPredictions = result["listPredictions"];
          this.listErrors = result["errors"];
          this.listCorrections = result["listCorrections"];
          console.log("getMassivePredictions : this.listPredictions = ", this.listPredictions, "listCorrections = ", this.listCorrections);
          console.log("before changeDetectorRef listPredictionsTotal =", this.listPredictionsTotal);
          //this.reload();
          var entropieByTrMatrixId = result['entropieByTrMatrixId'];
          var mapTrMatrixKey = result['mapTrMatrixKey'];
          this.entropieResult = {};
          console.log("getMassivePredictions : entropieByTrMatrixId",entropieByTrMatrixId );
          for(var matrixId in entropieByTrMatrixId) {
              var entropie = entropieByTrMatrixId[matrixId];
              var matrixKey = mapTrMatrixKey[matrixId];
              var matrixKey = mapTrMatrixKey[matrixId];
              var startHour = matrixKey['timeWindow']['startHour'];
              var endHour = matrixKey['timeWindow']['endHour'];
              console.log("next entropie", matrixId, matrixKey, startHour, entropie);
              var timeSlot = startHour + "H - " + endHour+ "H";
              this.entropieResult[timeSlot] = entropie;
          }
          console.log("entropieResult", this.entropieResult);

          var nb_actualTargetState = 0;
          //var shannonEntropie = 0;
          var nbOK_random = 0;
          var nbOK_likely = 0;
          var lastTargetHours = null;
          var lastInitialStateId = null;
          var sumSateProbabilities = [];
          var avgSateProbabilities = [];
          var arrayDifferentials = [];
          var divisor = 0;
          for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
            sumSateProbabilities.push(0.0);
            avgSateProbabilities.push(0.0);
          }
          for(var i = 0; i < this.listPredictions.length; i++) {
            var nextPrediction = this.listPredictions[i];
            if(nextPrediction.hasOwnProperty('mapLastResults')) {
              var mapLastResults = nextPrediction['mapLastResults'];
              //console.log("getMassivePredictions", i, "mapLastResults", mapLastResults);
              for (var variableName in mapLastResults) {
                // check if the property/key is defined in the object itself, not in parent
                if (mapLastResults.hasOwnProperty(variableName)) {
                    //console.log("step1", variableName, mapLastResults[variableName]);
                    var predictionResult = mapLastResults[variableName];
                    for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
                      sumSateProbabilities[state_idx] = sumSateProbabilities[state_idx]
                          + predictionResult['stateProbabilities'][state_idx];
                    }
                    divisor++;
                    var initialValue = nextPrediction['initialValues'][variableName];
                    var initialStateId = nextPrediction['initialStates'][variableName]['id'];
                    console.log("initialStateId", initialStateId);
                    predictionResult['initialValue'] = initialValue;
                    predictionResult['firstOfRowBloc'] = false;
                    var targetDate =  predictionResult['targetDate'];
                    var targetTime = (targetDate.split("T"))[1];
                    var targetHours = targetTime.substring(0,2);
                    //console.log("next target date : ", targetDate, targetTime, targetHours  );
                    if(lastTargetHours == null || (lastTargetHours != targetHours)) {
                      predictionResult['firstOfRowBloc'] = true;
                      //console.log("first of bloc");
                    }
                    if(initialStateId == null || (lastInitialStateId != initialStateId)) {
                      predictionResult['firstOfRowBloc'] = true;
                      //console.log("first of bloc");
                    }
                    //console.log("actualStatesStatistic = ", predictionResult['actualStatesStatistic']);
                    if(predictionResult['actualStatesStatistic'] != null) {
                      arrayDifferentials.push(predictionResult['differential']);
                    }
                    lastTargetHours = targetHours;
                    lastInitialStateId = initialStateId;
                    //console.log("getMassivePredictions", i, variableName, predictionResult);
                    // console.log("getMassivePredictions", i , "initialState = ", predictionResult['initialState']);
                    this.listPredictionItems.push(predictionResult);
                    if(predictionResult['actualTargetState'] != null) {
                      nb_actualTargetState++;
                      if(predictionResult['randomOK']) {
                        nbOK_random++;
                      }
                      if(predictionResult['mostLikelyStateOK']) {
                        nbOK_likely++;
                      }
                    }
                    //console.log("nextPrediction[actualStatesStatistic]", predictionResult["actualStatesStatistic"]);
                }
              }
            }
          }
          for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
            avgSateProbabilities[state_idx] = sumSateProbabilities[state_idx]/divisor;
          }
          console.log("arrayDifferentials=", arrayDifferentials);
          this.listPredictionsTotal={"nb":this.listPredictionItems.length
            ,"nb_actualTargetState":nb_actualTargetState
            ,"nbOK_random":nbOK_random
            ,"nbOK_likely":nbOK_likely
            ,"rate_random":(nb_actualTargetState==0)?0:nbOK_random/nb_actualTargetState
            ,"rate_likely":(nb_actualTargetState==0)?0:nbOK_likely/nb_actualTargetState
            ,"avgSateProbabilities":avgSateProbabilities
            ,"avgDifferentials" : arrayDifferentials.reduce((a, b) => a + b, 0) / arrayDifferentials.length
          }
          console.log("listPredictionsTotal = ", this.listPredictionsTotal);
          //this.nbPredictionItems = this.listPredictionItems.length;
          this.changeDetectorRef.detectChanges();
        })
  }

formatTimeSlot(timeSlot) {
  var date1 = new Date(timeSlot.startDate);
  var date2 = new Date(timeSlot.endDate);
  //console.log("formatTimeSlot", sdate1, sdate2);
  var day1 = formatDate(date1);
  var time1 = formatTime(date1);
  var day2 = formatDate(date2);
  var time2 = formatTime(date2);
  var markovWindow = "  (MTW:" + timeSlot.markovTimeWindow.id+")";
  //console.log("formatTimeSlot", day1, day2);
  if(day1 == day2)   {
    return day1 + "  " +  time1 + "-" + time2 + markovWindow;
  } else {
    return day1 + "  " +  time1 + "-" + day2 + " " + time2 + markovWindow;
  }
}

fnum_minus_plus(num, decimalNb, displayZero=false) {
  return fnum_minus_plus(num, decimalNb, displayZero);
}

fnum0(num, displayZero=false) {
  return fnum0(num, displayZero);
}

fnum2(num, displayZero=false) {
  return fnum2(num, displayZero);
}

  fnum3(num, displayZero=false) {
   return fnum3(num, displayZero)
  }

  reload() {
    console.log("--- refresh page");
    location.reload()
  }


  ngOnInit() {
  }

  ngOnChanges() {
    console.log("ngOnChanges");
  }

  changeDisplay(idSpanBlock) {
    var spanBLock = document.getElementById(idSpanBlock);
    var classesSpanBlock = spanBLock.classList;
    var toDisplay = classesSpanBlock.contains("display_none");
    console.log("displayAllOffers",  idSpanBlock, spanBLock, classesSpanBlock, toDisplay);
    spanBLock.className = toDisplay? 'display_yes' : 'display_none';
  }

  displayTimeSteps() {
    var spanElement = document.getElementById('ngc_time_steps');
    var toDisplay = (spanElement.className=='display_none');
    spanElement.className = toDisplay? 'display_yes' : 'display_none';
  }

  formatDate(sdate) {
    var date = new Date(sdate);
    return formatDate(date);
  }

  formatTime(sdate) {
    var date = new Date(sdate);
    return formatTime(date);
  }

  formatDateTime(sdate) {
    var date = new Date(sdate);
    return formatDate(date) + "  " + formatTime(date);
  }

  formatOnlyTime(sdate) {
    var date = new Date(sdate);
    return formatTime2(date);
  }

  getNodeLocationLabel(nodeLocation) {
    var prefix = "neighbor"
    if(this.defaultNodeLocation["name"] == nodeLocation.name) {
      prefix = "home";
    }
    return prefix+" " + nodeLocation.name;
  }
}
