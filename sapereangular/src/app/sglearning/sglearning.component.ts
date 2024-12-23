import { Component, OnInit, OnChanges, ChangeDetectorRef, ViewChild, ElementRef, Input, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders  } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { catchError } from 'rxjs/operators'
import { fnum_minus_plus, fnum0, fnum2,fnum3, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow, aux_formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay, getDefaultTimeZone } from '../common/util.js';
@Component({
  selector: 'app-sglearning',
  templateUrl: './sglearning.component.html',
  styleUrls: ['./sglearning.component.scss'],
  encapsulation: ViewEncapsulation.None
})


export class SGLearningComponent implements OnInit  {

  private margin: any = { top: 50, bottom: 50, left: 50, right: 20};
  private mapNodeTransitionMatrices = {};
  private mapTrMatrix = {};
  private useNewMKCmodel = true;
  private mapLstmMatrices = {};
  private variables = [];
  private all_variables = [];
  private modelIterations = [];
  private listTimeWindows = [];
  private listStates = [];
  private listScopes = [];
  private listScopesAggregation = [];
  private stateNb = 0;
  private modelType = "";
  private sTimeWindow = "";
  private prediction = {};
  private aggregationResult = {};
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
  private predictionContext = {};
  private listYesNo = [{"label":"no", "value":false}, {"label":"yes", "value":true} ];
  private displaySuccessRate = true;
  private selectedScope = {"value":"-1", "label": "NODE"} ;//"NODE";//"CLUSTER";
  private displayLSTMmatrices = false;
  private listLayers = [];

  private scopeFilter = {
    "scope":this.selectedScope
  };

  // form for Red-avg checkuo
  private aggregationCheckupRequest = {
    "variableName":""
    ,"scope":this.selectedScope
  };

  // form for statistic request
  private statisticsRequest = {
     "minComputeDay_ddMMyyyy":getDefaultTargetDay()
    ,"maxComputeDay_ddMMyyyy":getDefaultTargetDay()
    ,"minTargetDay_ddMMyyyy":null
    ,"maxTargetDay_ddMMyyyy":null
    ,"nodeName":""
    ,"nodeLocation":{}
    ,"mergeHorizons":true
    ,"mergeUseCorrections":false
    ,"minPredictionsCount":5
    ,"includeTrivials":true
    ,"mergableFields":[
          {"value":"horizon", "label":"horizon"}
        , {"value":"useCorrection","label":"use correction"}
        , {"value":"hour", "label":"time slot"}]
    ,"fieldsToMerge":["horizon"]
    ,"scope":this.selectedScope
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
      ,"scope":this.selectedScope
    }
  // form for massive prediction request
  private massivePredictionRequest = {
      "targetDay_ddMMyyyy":getDefaultTargetDay()
     ,"targetHour":Math.max(0,getDefaultHour()-2)
     ,"horizonInMinutes":5
     ,"nodeLocation":{}
     ,"nodeName":""
     ,"listVariableNames":[]
     ,"useCorrections":false
     ,"generateCorrections":false
     ,"savePredictions":true
     ,"scope":this.selectedScope
    };
  // filter for matrix refresh
  private matrixFilter = {
       "nodeLocation":{}
      ,"nodeName":""
      ,"listVariableNames":[]
      ,"startHourMin":getDefaultHour()
      ,"startHourMax":(1+getDefaultHour())
      ,"includeCorrections":true
      ,"scope":this.selectedScope
    }
  private displayDistanceVector = false; //"display_none"; // display_yes

  constructor(private httpClient: HttpClient,private _constant: ConstantsService, public _chartElem: ElementRef, private cd: ChangeDetectorRef) {
    this.changeDetectorRef = cd;
    //this.changeDetectorRef.detectChanges();
    this.httpClient.get(this._constant.baseAppUrl+'energy/getNodeLocation').
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
    console.log("FOR DEBUG scopeParam = ", this.scopeFilter);
    this.httpClient.post(this._constant.baseAppUrl+'energy/getMapAllNodeLocations', this.scopeFilter).
      subscribe((result :any[])=> {
        this.mapAllNodeLocations = result;
        Object.entries(this.mapAllNodeLocations).forEach(([key, value]) => {
          console.log('------- getMapAllNodeLocations Key : ' + key + ', Value : ' + value["mainServiceAddress"], value["url"])
        })
      });

    console.log("step 0000000  : beffore call to ", this._constant.baseAppUrl+'energy/getPredictionContext', " defaultNodeLocation= ", this.defaultNodeLocation);    
    this.httpClient.post(this._constant.baseAppUrl+'energy/getPredictionContext',  this.scopeFilter).
        subscribe((result :any[])=> {
          console.log('------- TEST1 ', result);
          this.predictionContext = result;
          this.listScopes = this.predictionContext["listScopeItems"];
          this.listScopesAggregation = this.predictionContext["listScopeItemsHavingAggregation"];
          //console.log("listScopesAggregation = ", this.listScopesAggregation);
          if(this.listScopesAggregation.length == 1) {
            var firstItem = this.listScopesAggregation [0];
            //console.log("first scope = ", firstItem);
            this.aggregationCheckupRequest["scope"] = firstItem;
          }
          console.log(" listScopes = ", this.listScopes);
          console.log('------- TEST2 predictionContext  : ' , this.predictionContext );
          var listTimeWindow = this.predictionContext["allTimeWindows"];
          var defaultHour = getDefaultHour();
          for(var idx=0; idx <listTimeWindow.length; idx++ ) {
            var nextTW = listTimeWindow[idx];
            console.log("next TW : ", nextTW);
            if(defaultHour == nextTW.startHour) {
              console.log("default TW found");
              this.aggregationCheckupRequest["timeWindowId"] = nextTW.id;
            }
          }
          this.all_variables = this.predictionContext["nodeContext"]["variables"];
          //this.variables = this.predictionContext["nodeContext"]["variables"];
          console.log("this.all_variables = ", this.all_variables);

         this.listStates = [];
         this.modelType = "";
          console.log("after getPredictionContext : stateList = ", this.predictionContext["statesList"]);
          if(this.predictionContext["statesList"] !== undefined) {
            this.listStates = this.predictionContext["statesList"];
          }
          this.stateNb = this.listStates.length;
          if(this.predictionContext["modelType"] !== undefined) {
            this.modelType = this.predictionContext["modelType"];
          }
        });
      console.log("step 11111111   : predictionContext = " , this.predictionContext);

      this.httpClient.post(this._constant.baseAppUrl+'energy/getStateDates', this.scopeFilter).
      subscribe((result :any[])=> {
        this.listStateDates = result;
        var minTargetDate = null;
        var maxTargetDate = null;
        console.log("this.listStateDates = ", this.listStateDates);
        for(var idx=0; idx <this.listStateDates.length; idx++ ) {
          var nextDate = this.listStateDates[idx]["value"];
          if(minTargetDate == null || nextDate  < minTargetDate) {
            minTargetDate = nextDate;
          }
          if(maxTargetDate == null || nextDate  > maxTargetDate) {
            maxTargetDate = nextDate;
          }
        }
        this.statisticsRequest["minTargetDay_ddMMyyyy"] = minTargetDate;
        this.statisticsRequest["maxTargetDay_ddMMyyyy"] = maxTargetDate;
        this.singlePredictionRequest["initDay"] = minTargetDate;
        this.singlePredictionRequest["targetDay"] = minTargetDate;
        console.log("getStateDates listStateDates :" , this.listStateDates);
      });
    console.log("before refreshMatrices  : predictionContext = " , this.predictionContext);
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
    if(this.useNewMKCmodel) {
      var variableMapTrMatrix = this.mapTrMatrix[variableName];
      if(variableMapTrMatrix === undefined) {
        return undefined;
      }
      return variableMapTrMatrix[timeWindow];
    }
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

  aux_getNode(aggregationResult, agentName) {
    console.log("aux_getNode : aggregationResult = ", aggregationResult, "agentName = ", agentName);
    var mapNodes = aggregationResult["mapNodeByAgent"];
    //console.log("aux_getNode mapNodes = ", mapNodes);
    return mapNodes[agentName];
  }

  aux_displayAllObsMatrixRowSum(trMatrix, rowId) {
    var row = this.aux_selectAllObsMatrixRow(trMatrix, rowId);
    var obsNb = this.computeRowSum(row);
    var result ="" + this.formatObsNumber(obsNb);
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
    return "" + this.formatObsNumber(matrix.array[rowId][columnId]);
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

  auxGetTotalModelWeight(aggregationResult) {
    var aggregationWeights = aggregationResult['aggregationWeights']
    var result = 0.0;
    for (const [key, value] of Object.entries(aggregationWeights)) {
      //console.log("auxGetTotalModelWeight", `${key}: ${value}`);
      result+= aggregationWeights[key];
    }
    return result;
  }


  auxGetModelWeight(aggregationResult, node) {
    var aggregationWeights = aggregationResult['aggregationWeights']
    //console.log("auxGetModelWeight : aggregationWeights = " + aggregationWeights + ", node = ", node, aggregationWeights[node]);
    return aggregationWeights[node];
  }

 formatObsNumber(obsNb) {
  var decPart = obsNb - Math.floor(obsNb);
  var result = "" + obsNb;
  if(Math.abs(decPart) > 0.0001) {
    result = fnum2(obsNb);
  } else {
    result = fnum0(obsNb);
  }
  return result;
 }


 displayAllObsMatrixRowSum(variableName, timeWindow, rowId) {
  var obsNb = this.computeRowSum(this.selectAllObsMatrixRow(variableName, timeWindow, rowId));
  return this.formatObsNumber(obsNb);
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



  selectStateProbabilitiesFromPartialAggregationResult(simppleResult, node) {
    console.log("selectStateProbabilitiesFromPartialAggregationResult simppleResult = ", simppleResult, node);
    console.log("nodesStateProbabilities = ", simppleResult["nodesStateProbabilities"]);
    var nodeStateProbabilities = simppleResult["nodesStateProbabilities"][node];
    return nodeStateProbabilities;
  }



  selectMatrixFromAggregationResult(node, matrixKey) {
    //console.log("selectMatrixFromAggregationResult : node = ", node, ", nodeLayers = ", this.aggregationResult["nodeLayers"]);
    var nodeResult = this.aggregationResult["nodeLayers"][node]["mapMatrices"];
    //console.log("selectMatrixFromAggregationResult : nodeResult = ", nodeResult);
    var matrix = nodeResult[matrixKey];
    //console.log("selectMatrixFromAggregationResult : matrix = " , matrix);
    return matrix;
  }

  selectMatrixFromAggregationResultArray(node, matrixKey) {
    var matrix = this.selectMatrixFromAggregationResult(node, matrixKey);
    return matrix["array"];
  }

  selectMatrixFromAggregationResultRow(node, matrixKey, rowIdx) {
    console.log("selectMatrixFromAggregationResultRow", node, matrixKey, rowIdx);
    var matrix = this.selectMatrixFromAggregationResult(node, matrixKey);
    return matrix["array"][rowIdx];
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

  geClassSuccessRate(rate) {
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

  refreshMatrices(){
    var matrixFilter = this.matrixFilter;
    //this.stateNb = this.listStates.length;
    console.log("refreshMatrices : matrixFilter = ", this.matrixFilter, "predictionContext = ", this.predictionContext);
    //console.log("refreshMatrices : scope = ", this.matrixFilter["scope"], "this.defaultScope = ", this.selectedScope);
    //console.log("refreshMatrices : predictionContext = ", this.predictionContext);
    var chosenNodeLocation = this.defaultNodeLocation;
    var chosenNodeName = matrixFilter["nodeName"];
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    if(chosenNodeName  != "" && this.mapAllNodeLocations.hasOwnProperty(chosenNodeName)) {
      console.log("refreshMatrices : chosenNodeName = ", chosenNodeName);
      console.log("refreshMatrices : mapAllNodeLocations = ", this.mapAllNodeLocations);
      chosenNodeLocation = this.mapAllNodeLocations[chosenNodeName];
      //matrixFilter["nodeLocation"] = chosenNodeLocation;
      console.log("refreshMatrices : chosenNodeLocation = ", chosenNodeLocation);
      matrixFilter["nodeName"] = chosenNodeLocation["name"];
      chosenBaseUrl = chosenNodeLocation["url"];
    } else {
      console.log("init condition : defaultNodeLocation = ",  this.defaultNodeLocation);
    }
    console.log("refreshMatrices : matrixFilter = ", this.matrixFilter
      , "chosenNodeLocation = ", chosenNodeLocation
      , "chosenBaseUrl = ", chosenBaseUrl);
    //var sep = chosenBaseUrl.endsWith("/")? "" : "/";
    var serviceUrl = chosenBaseUrl + "/" + 'allNodeTransitionMatrices';
    console.log("refreshMatrices : calling service 1", serviceUrl, chosenBaseUrl);
    console.log("all_variables = ", this.all_variables);
    this.httpClient.post(serviceUrl,  this.matrixFilter ).
    //this.httpClient.post(serviceUrl, this.matrixFilter , { responseType: 'json' , headers:apiHeader}).
      subscribe((receivedModel :any[])=> {
        console.log("subscribe result = ", receivedModel);
        this.variables = receivedModel["usedVariables"];
        this.statisticsRequest["listVariableNames"] = this.variables;
        this.massivePredictionRequest["listVariableNames"] = this.variables;
        console.log("this.variables : ", this.variables);
        this.listTimeWindows = [];
        if(this.modelType == "MARKOV_CHAINS") {
          this.modelIterations = receivedModel["iterations"];
          //var nbOfIterations = this.modelIterations.length;
          console.log("refreshMatrices : modelIterations = ", this.modelIterations);
          if(this.useNewMKCmodel) {
              var mapModels =  receivedModel["mapModels"];
              //console.log("refreshMatrices : mapModels = ", mapModels);
              var firstVar = true;
              this.listTimeWindows = [];
              for (const [variable, varModel] of Object.entries(mapModels)) {
                var mapVariableTrMatrix = {};
                var mapMatrices = varModel["mapMatrices"];
                console.log("mapMatrices = ", mapMatrices);
                for (const [key, trMatrix] of Object.entries(mapMatrices)) {
                  var timeWindow = trMatrix["featuresKey"]["timeWindow"];
                  var sTimeWindow = aux_formatTimeWindow(timeWindow);
                  if(firstVar) {
                    this.sTimeWindow = sTimeWindow;
                    this.listTimeWindows.push(sTimeWindow);
                  }
                  //console.log("sTimeWindow = ", sTimeWindow);
                  mapVariableTrMatrix[sTimeWindow] = trMatrix;
                  //console.log(variable, key, sTimeWindow, trMatrix);
                }
                this.mapTrMatrix[variable] = mapVariableTrMatrix;
                firstVar = false;
              }
          } else {
            var mapContent = receivedModel["content"];
            console.log("refreshMatrices : mapContent = ", mapContent);
            var listNodeTransitionMatrices = [];
            for (const [key, value] of Object.entries(mapContent)) {
               console.log(`${key}: ${value}`);
               listNodeTransitionMatrices.push(value);
            }
            console.log("refreshMatrices : listNodeTransitionMatrices = ", listNodeTransitionMatrices);
            for(var idx in listNodeTransitionMatrices) {
              var nodeTransitionMatrices = listNodeTransitionMatrices[idx];
              var sTimeWindow = formatTimeWindow(nodeTransitionMatrices);
              this.sTimeWindow = sTimeWindow;
              this.listTimeWindows.push(sTimeWindow);
              this.mapNodeTransitionMatrices[sTimeWindow] = nodeTransitionMatrices;
            }
          }
        }
        if(this.modelType == "LSTM") {
          this.mapLstmMatrices = {};
          var mapModels = receivedModel["mapModels"];
          console.log("LSTM");
          var firstVar = true;
          this.listLayers = [];
          for (const [nextVariable, varModel] of Object.entries(mapModels)) {
            var layers = varModel["layers"];
            console.log("variable = ", nextVariable, "layers = ", layers);
            this.mapLstmMatrices[nextVariable] = {};
            for(var idx_layer in layers) {
              var nextLayer = layers[idx_layer];
              var layerMatrices = nextLayer["mapMatrices"];
              var layerDefinition = nextLayer["layerDefinition"];
              var layerKey2 = layerDefinition["key2"];
              var nbMatrices = Object.keys(layerMatrices).length;
              console.log("layerMatrices = ", layerMatrices, "nbMatrices = ", nbMatrices);
              if(firstVar && nbMatrices > 0) {
                this.listLayers.push(layerKey2);
              }
              this.mapLstmMatrices[nextVariable][layerKey2] = {};
              for (const [matrix_key, matrix] of Object.entries(layerMatrices)) {
                this.mapLstmMatrices[nextVariable][layerKey2][matrix_key] = matrix;
              }
            }
            firstVar = false;
            console.log("list_layers = ", this.listLayers);
          }
        }
        console.log("this.mapLstmMatrices", this.mapLstmMatrices);
        this.changeDetectorRef.detectChanges();
        console.log("change detector", this.changeDetectorRef);
        console.log("variables", this.variables);
        console.log("listStates", this.listStates, this.stateNb);
        console.log("listTimeWindows", this.listTimeWindows);
        console.log("mapHomeTransitionMatrices", this.mapNodeTransitionMatrices);
        console.log("mapTrMatrix =", this.mapTrMatrix);
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
    //this.httpClient.get(serviceUrl, this.singlePredictionRequest, { responseType: 'json' }).
    this.httpClient.post(serviceUrl, this.singlePredictionRequest).
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

  checkupModelAggregation() {
    console.log("checkupModelAggregation : aggregationCheckupRequest = ", this.aggregationCheckupRequest);
    var chosenBaseUrl = this._constant.baseAppUrl + "energy";
    var serviceUrl = chosenBaseUrl+'/checkupModelAggregation';
    console.log("checkupModelAggregation : calling service", serviceUrl);
    this.httpClient.post(serviceUrl, this.aggregationCheckupRequest).
      subscribe((result :any[])=> {
        this.aggregationResult = result;
        console.log("checkupModelAggregation : aggregationResult = ", this.aggregationResult);
        var nodeName = this.defaultNodeLocation["name"];
        console.log("nodeName = ", nodeName);
        if(this.aggregationResult['nodeTransitionMatrices'] !== undefined) {
          var maptrMatrices = this.aggregationResult['nodeTransitionMatrices'];
          var trMatrix = maptrMatrices[nodeName];
          console.log("trMatrix :", trMatrix);
          var idxState = 0;
          var row = this.aux_selectNormalizedMatrixRow(trMatrix, idxState, true);
          console.log("first row :", row);
        }
        if(this.aggregationResult['aggregatedLayer'] !== undefined) {
          var mapMatrices = this.aggregationResult['aggregatedLayer']["mapMatrices"];
          console.log("mapMatrices", mapMatrices);
          for (var key in mapMatrices){
            console.log("next key", key);
          }
          //var testN1 = this.selectMatrixFromAggregationResultArray("Learning_agent_N1", "b");
          //var testN2 = this.selectMatrixFromAggregationResultArray("Learning_agent_N2", "b");
          //console.log("testN1 = ", testN1);
          //console.log("testN2 = ", testN2);
        }
        if(this.aggregationResult['mapReults'] !== undefined) {
          var mapNodeResults = this.aggregationResult['mapReults'];
          console.log("mapNodeResults = ", mapNodeResults);
        }
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
    //var minComputeDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["minComputeDay_ddMMyyyy"], "00:00");
    console.log("getStatistics : default_timezone = ", default_timezone)
    var default_timezone = getDefaultTimeZone();
    //default_timezone = "+01:00"
    // Compute day
    var s_minComputeDay = this.statisticsRequest["minComputeDay_ddMMyyyy"] + "T00:00:00";
    this.statisticsRequest["minComputeDay"] = s_minComputeDay;
    var s_maxComputeDay = this.statisticsRequest["maxComputeDay_ddMMyyyy"] + "T00:00:00";
    this.statisticsRequest["maxComputeDay"] = s_maxComputeDay;
    //Target day
    var s_minTargetDay = this.statisticsRequest["minTargetDay_ddMMyyyy"] + "T00:00:00";
    this.statisticsRequest["minTargetDay"] = s_minTargetDay;
    var s_maxTargetDay = this.statisticsRequest["maxTargetDay_ddMMyyyy"] + "T00:00:00";
    this.statisticsRequest["maxTargetDay"] = s_maxTargetDay;

    //this.statisticsRequest["maxComputeDay"] = timeYYYYMMDDHMtoDate(this.statisticsRequest["minComputeDay_ddMMyyyy"], "00:00");


    //var maxComputeDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["maxComputeDay_ddMMyyyy"], "00:00");
    //var minTargetDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["minTargetDay"], "00:00");
    //var maxTargetDay =  timeYYYYMMDDHMtoDate(this.statisticsRequest["maxTargetDay"], "00:00");
    //console.log("getStatistics1 : minComputeDay = ", minComputeDay, " maxComputeDay = ", maxComputeDay, "s_minComputeDay = ", s_minComputeDay);
    console.log("getStatistics1 : s_minComputeDay = ", s_minComputeDay, " s_maxComputeDay = ", s_maxComputeDay);
    //this.statisticsRequest["longMinComputeDay"] = minComputeDay.getTime();
    //this.statisticsRequest["longMaxComputeDay"] = maxComputeDay.getTime();
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
    console.log("getStatistics1 : calling service", serviceUrl
    , "generateCorrections = ", this.statisticsRequest["generateCorrections"]
    , "savePredictions = ", this.statisticsRequest["savePredictions"]);
    this.httpClient.post(serviceUrl, this.statisticsRequest)
    .subscribe(
      (result :any[])=> {
          console.log("getStatistics : result = ", result);
          this.listStatisitcs = result;
          //this.listErrors = result["errors"];
          console.log("getStatistics : this.listStatisitcs = ", this.listStatisitcs, "listErrors = ", this.listErrors);
          console.log("before changeDetectorRef");
          console.log("fieldsToMerge", this.statisticsRequest['fieldsToMerge'])
          var weightedEntropie = 0.0;
          var weightedGiniIdx = 0.0;
          var weightedReliability = 0.0;
          var weightedReliabilityNonTrivial = 0.0;
          var weightedSuccessRateNonTrivial = 0.0;
          var weightedCrossEntropyLoss = 0.0;
          var listComputeDays = [];
          this.totalStatistics = {'nbOfPredictions':0, 'nbOfPredictions2':0, 'nbOfTrivialPredictions':0, 'nbOfNonTrivialPredictions':0, 'trivialPredictionsRate':0
            , 'differential' : 0.0, 'reliability':0.0, 'reliabilityNonTrival' : 0.0, 'successRateNonTrivial': 0.0
            , 'nbOfSuccesses':0, 'nb':0, 'arrayMeansOfProba':[], 'arraySumsOfProba':[], 'timeSlot':{'beginDate':null, 'endDate':null}};
          for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
            this.totalStatistics['arrayMeansOfProba'][state_idx] = 0;
            this.totalStatistics['arraySumsOfProba'][state_idx] = 0;
          }
          var lastBeginHours = null;
          console.log("this.listStatisitcs.length = ", this.listStatisitcs.length);
          for(var i = 0; i < this.listStatisitcs.length; i++) {
            var nextStatistic = this.listStatisitcs[i];
            var beginDate =  nextStatistic['timeSlot']['beginDate'];
            var endDate =  nextStatistic['timeSlot']['endDate'];
            if(i == 0) {
              this.totalStatistics["scope"] = nextStatistic["scope"];
              this.totalStatistics["clusterConfig"] = nextStatistic["clusterConfig"];
              this.totalStatistics["aggregationOperator"] = nextStatistic["aggregationOperator"];
              this.totalStatistics["timeSlot"]["beginDate"] = beginDate;
            }
            var nextComputeDay = nextStatistic["computeDay"];
            if(!listComputeDays.includes(nextComputeDay)) {
              listComputeDays.push(nextComputeDay)
            }
            console.log("nextStatistic = ", nextStatistic);
            nextStatistic['firstOfRowBloc'] = false;
            //console.log("beginDate = ", beginDate);
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
              weightedEntropie+= nextStatistic['statesStatistic']['shannonEntropie'] * nextStatistic['nbOfPredictions'];
              weightedGiniIdx+= nextStatistic['statesStatistic']['giniIndex'] * nextStatistic['nbOfPredictions'];
            }
            //var arrayMeansOfProba = nextStatistic['arrayMeansOfProba'];
            //console.log("arrayMeansOfProba = ", arrayMeansOfProba);
            for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
              var toAdd =  nextStatistic['nbOfPredictions'] * nextStatistic['arrayMeansOfProba'][state_idx];
              this.totalStatistics['arraySumsOfProba'][state_idx] = this.totalStatistics['arraySumsOfProba'][state_idx] + toAdd ;
            }
            //console.log("arraySumsOfProba = ", this.totalStatistics['arraySumsOfProba']);
            if(nextStatistic['differential'] != null) {
              weightedReliability+= (nextStatistic['reliability']) * nextStatistic['nbOfPredictions'];
              this.totalStatistics['nbOfPredictions2'] =  this.totalStatistics['nbOfPredictions2'] + nextStatistic['nbOfPredictions'];
              if(nextStatistic['trivial']) {
                this.totalStatistics['nbOfTrivialPredictions'] =  this.totalStatistics['nbOfTrivialPredictions'] + nextStatistic['nbOfPredictions'];
              } else {
                this.totalStatistics['nbOfNonTrivialPredictions'] =  this.totalStatistics['nbOfNonTrivialPredictions'] + nextStatistic['nbOfPredictions'];
                console.log("add to weightedReliabilityNonTrivial reliability = ", nextStatistic['reliability'] + ", nbOfPredictions = " + nextStatistic['nbOfPredictions']);
                console.log("add to weightedReliabilityNonTrivial : ", nextStatistic['reliability'] * nextStatistic['nbOfPredictions']);
                weightedReliabilityNonTrivial += (nextStatistic['reliability'] * nextStatistic['nbOfPredictions']);
                weightedSuccessRateNonTrivial += (nextStatistic['successRate'] * nextStatistic['nbOfPredictions']);
              }
            }
            if(nextStatistic['crossEntropyLoss'] != null) {
              weightedCrossEntropyLoss+=nextStatistic['crossEntropyLoss'] * nextStatistic['nbOfPredictions'];
            }
          }
          console.log("totalStatistics = ", this.totalStatistics);
          this.totalStatistics["computeDay"] = listComputeDays; // listComputeDays.join(", ");
          this.totalStatistics['successRate'] = 0;
          this.totalStatistics['shannonEntropie'] = 0.0;
          this.totalStatistics['giniIndex'] = 0.0;
          this.totalStatistics['differential'] = 0.0;
          this.totalStatistics['crossEntropyLoss'] = 0.0;
          this.totalStatistics["timeSlot"]["endDate"] = endDate;
          if(this.totalStatistics['nbOfPredictions'] > 0 ) {
            var nbOfPredictions = this.totalStatistics['nbOfPredictions'];
            this.totalStatistics['successRate'] = this.totalStatistics['nbOfSuccesses'] / nbOfPredictions;
            this.totalStatistics['shannonEntropie'] = weightedEntropie /  nbOfPredictions;
            this.totalStatistics['giniIndex'] = weightedGiniIdx / nbOfPredictions;
            for(var state_idx = 0; state_idx < this.stateNb; state_idx++) {
              this.totalStatistics['arrayMeansOfProba'][state_idx] = this.totalStatistics['arraySumsOfProba'][state_idx] / nbOfPredictions;
            }
            var nbOfPredictions2 = this.totalStatistics['nbOfPredictions2'];
            this.totalStatistics['reliability'] = weightedReliability / nbOfPredictions2;
            this.totalStatistics['crossEntropyLoss'] = weightedCrossEntropyLoss / nbOfPredictions2;
            this.totalStatistics['trivialPredictionsRate'] = this.totalStatistics['nbOfTrivialPredictions'] / nbOfPredictions;
            var nbOfNonTrivialPredictions = nbOfPredictions - this.totalStatistics['nbOfTrivialPredictions']
            if(nbOfNonTrivialPredictions > 0) {
              console.log("weightedReliabilityNonTrivial = ", weightedReliabilityNonTrivial, " weightedSuccessRateNonTrivial=", weightedSuccessRateNonTrivial, " nbOfNonTrivialPredictions = ", nbOfNonTrivialPredictions);
              this.totalStatistics['reliabilityNonTrival'] = weightedReliabilityNonTrivial / nbOfNonTrivialPredictions;
              this.totalStatistics['successRateNonTrivial'] = weightedSuccessRateNonTrivial / nbOfNonTrivialPredictions;
            }
            console.log("nbOfNonTrivialPredictions = ", nbOfNonTrivialPredictions, ", weightedReliabilityNonTrivial = ", weightedReliabilityNonTrivial);
          }
          console.log(" totalStatistics = ", this.totalStatistics);
          //console.log("arraySumsOfProba = ", this.totalStatistics['arraySumsOfProba']);
          this.changeDetectorRef.detectChanges();
          this.listErrors = [];
        },

        (error) => {
          //Error callback
          console.error('error caught in component', error)
          console.log("error.error", error.error);
          this.listErrors[0] = error.error;
          }
        )
  }


  public handleError(error: Response) {
    console.log("handleError", error);
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


    var s_targetDay = this.massivePredictionRequest["targetDay_ddMMyyyy"] + "T00:00:00";
    this.massivePredictionRequest["targetDay"] = s_targetDay;
    console.log("getMassivePredictions : s_targetDay = ", s_targetDay);
    var serviceUrl = chosenBaseUrl+'/getMassivePredictions';
    this.httpClient.post(serviceUrl, this.massivePredictionRequest).
    subscribe((result :any[])=> {
          console.log("getMassivePredictions : result = ", result);
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
              var startHour = matrixKey['featuresKey']['timeWindow']['startHour'];
              var endHour = matrixKey['featuresKey']['timeWindow']['endHour'];
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
                    console.log("targetDate = ", targetDate);
                    var targetTime = (targetDate.split(" "))[1];
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

formatTimeSlot(predictionStep) {
  console.log("formatTimeSlot : timeSlot = ", predictionStep);
  var date1 = new Date(predictionStep.startDate);
  var date2 = new Date(predictionStep.endDate);
  //console.log("formatTimeSlot", sdate1, sdate2);
  var day1 = formatDate(date1);
  var time1 = formatTime(date1);
  var day2 = formatDate(date2);
  var time2 = formatTime(date2);
  var timeWindow = "  (MTW:" + predictionStep.featuresKey.timeWindow.id+")";
  //console.log("formatTimeSlot", day1, day2);
  if(day1 == day2)   {
    return day1 + "  " +  time1 + "-" + time2 + timeWindow;
  } else {
    return day1 + "  " +  time1 + "-" + day2 + " " + time2 + timeWindow;
  }
}

aux_formatTimeWindow(timeWindow) {
  return aux_formatTimeWindow(timeWindow);
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

  formatDate2(sdate) {
    var date = new Date(sdate);
    return formatDate(date);
  }

  formatListDates(arrayDates) {
    var result = "";
    var sep = "";
    for (let i = 0; i < arrayDates.length; i++) {
      var nextDate = arrayDates[i];
      console.log("formatListDates : nextDate = ", nextDate, typeof nextDate);
      var date = new Date(nextDate);
      result = result + sep + formatDate(date);
      sep = ", ";
    }
    return result;
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
/*
  displayTimeWowLabel(timeWindow) {
    var startHour2d = (timeWindow.startHour < 10 ? "0" : "") + timeWindow.startHour + "";
    var  startMinude2d = (timeWindow.startMinute < 10 ? "0" : "") + timeWindow.startMinute + "";
    var result = startHour2d + "-" + startMinude2d;
    return result;
  }
*/
}
