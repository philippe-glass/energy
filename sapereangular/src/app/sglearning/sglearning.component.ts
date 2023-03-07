import { Component, OnInit, OnChanges, ChangeDetectorRef, ViewChild, ElementRef, Input, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpParams  } from '@angular/common/http';
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
  private listLocations = [];
  private listStateDates = [];
  private listYesNo = [{"label":"no", "value":false}, {"label":"yes", "value":true} ];
  // form for statistic request
  private statisticsRequest = {
     "minComputeDay":getDefaultTargetDay()
    ,"maxComputeDay":getDefaultTargetDay()
    ,"minTargetDay":null
    ,"maxTargetDay":null
    ,"location":""
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
      ,"initDate":null
      ,"targetDay":getDefaultTargetDay()
      ,"targetTime": getDefaultTargetTime()
	    ,"targetDate":null
	    ,"location":""
	    ,"useCorrections":false
  }
  // form for massive prediction request
  private massivePredictionRequest = {
      "targetDay":getDefaultTargetDay()
     ,"targetHour":Math.max(0,getDefaultHour()-2)
     ,"horizonInMinutes":5
     ,"location":""
     ,"variableName":""
     ,"useCorrections":true
     ,"generateCorrections":false
  };
  // filter for matrix refresh
  private matrixFilter = {
       "location":""
      ,"variableName":""
      ,"startHourMin":getDefaultHour()
      ,"startHourMax":(1+getDefaultHour())
      ,"includeCorrections":true
  }
  private displayDistanceVector = false; //"display_none"; // display_yes

  constructor(private httpClient: HttpClient,private _constant: ConstantsService, public _chartElem: ElementRef, private cd: ChangeDetectorRef) {
    this.changeDetectorRef = cd;
    //this.changeDetectorRef.detectChanges();
    this.httpClient.get(this._constant.baseAppUrl+'energy/getLocations').
      subscribe((result :any[])=> {
        this.listLocations = result;
        console.log("this.listLocations = ", this.listLocations);
        if(this.listLocations.length>0) {
          var defaultLocation = (this.listLocations[0]).value;
          console.log("defaultLocation", defaultLocation);
          if(this.statisticsRequest['location'] == "") {
            this.statisticsRequest['location'] = defaultLocation;
          }
          if(this.singlePredictionRequest['location'] == "") {
            this.singlePredictionRequest['location'] = defaultLocation;
          }
          if(this.massivePredictionRequest['location'] == "")  {
            this.massivePredictionRequest['location'] = defaultLocation;
          }
          if(this.matrixFilter['location'] == "")  {
            this.matrixFilter['location'] = defaultLocation;
          }
        }
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


  selectNormalizedMatrixRow2(variableName, timeWindow, rowId) {
    var includeCorrections = this.doIncludeCorrections();
    //console.log("selectNormalizedMatrixRow2 variableName = ",variableName, " , includeCorrections = ", includeCorrections);
    return this.selectNormalizedMatrixRow(variableName, timeWindow, rowId, includeCorrections);
  }

  selectNormalizedMatrixRow(variableName, timeWindow, rowId, includeCorrections) {
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return [];
    }
    var norm_matrix = includeCorrections ? trMatrix.normalizedMatrix2 :  trMatrix.normalizedMatrix1;
    return norm_matrix.array[rowId];
  }

  selectAllObsMatrixRow(variableName, timeWindow, rowId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return [];
    }
    var matrix1 = trMatrix.allObsMatrix;
    return matrix1.array[rowId];
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
    if (trMatrix === undefined) {
      return [];
    }
    var matrix = trMatrix.allCorrectionsMatrix;
    return matrix.array[rowId];
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
    if (trMatrix === undefined) {
      return null;
    }
    var matrix = trMatrix.allObsMatrix;
    return matrix.array[rowId][columnId];
  }

  selectAllCorrectionsMatrixCell(variableName, timeWindow, rowId, columnId) {
    //console.log("selectNormalizedMatrixRow begin", variableName, timeWindow, rowId);
    var trMatrix = this.selectTransitionMatrix(variableName, timeWindow);
    if (trMatrix === undefined) {
      return null;
    }
    var matrix = trMatrix.allCorrectionsMatrix;
    return matrix.array[rowId][columnId];
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
    var row =  this.selectNormalizedMatrixRow(variableName, timeWindow, rowId, false);
    var sum = this.computeRowSum(row);
    if(sum==0) {
      return "warning_high";
    }
    return "";
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
    console.log("refreshMatrices : matrixFilter = ", this.matrixFilter);
    this.httpClient.post(this._constant.baseAppUrl+'energy/allNodeTransitionMatrices', this.matrixFilter , { responseType: 'json' }).
      subscribe((res :any[])=> {
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

  getPrediction(){
    this.singlePredictionRequest['initDate'] = timeYYYYMMDDHMtoDate(this.singlePredictionRequest['initDay'], this.singlePredictionRequest['initTime']);
    this.singlePredictionRequest['targetDate'] = timeYYYYMMDDHMtoDate(this.singlePredictionRequest['targetDay'], this.singlePredictionRequest['targetTime']);
    console.log("getPRedicton singlePredictionRequest = ", this.singlePredictionRequest);
    this.httpClient.post(this._constant.baseAppUrl+'energy/getPrediction', this.singlePredictionRequest, { responseType: 'json' }).
    subscribe(res => {
      this.prediction = res;
      console.log("getPrediction : this.prediction = ", this.prediction);
      console.log("before changeDetectorRef");
      //this.reload();
      if(this.prediction.hasOwnProperty('mapLastResults')) {
        var mapLastResults = this.prediction['mapLastResults'];
        console.log("mapLastResults", mapLastResults);
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
    console.log("getStatistics1 : useCorrectionsFilter = ", this.statisticsRequest['useCorrectionsFilter'], typeof this.statisticsRequest['useCorrectionsFilter']);
    this.httpClient.post(this._constant.baseAppUrl+'energy/computePredictionStatistics', this.statisticsRequest, { responseType: 'json' }).
    subscribe((result :any[])=> {
          this.listStatisitcs = result;
          //this.listErrors = result["errors"];
          console.log("getStatistics : this.listPredictions = ", this.listPredictions, "listErrors = ", this.listErrors);
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
          for(var i = 0; i < this.listStatisitcs.length; i++) {
            var nextStatistic = this.listStatisitcs[i];
            nextStatistic['firstOfRowBloc'] = false;
            var beginDate =  nextStatistic['timeSlot']['beginDate'];
            var beginTime = (beginDate.split("T"))[1];
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
    this.httpClient.post(this._constant.baseAppUrl+'energy/getMassivePredictions', this.massivePredictionRequest, { responseType: 'json' }).
    subscribe((result :any[])=> {
          this.listPredictionItems = [];
          this.listPredictions = result["listPredictions"];
          this.listErrors = result["errors"];
          this.listCorrections = result["listCorrections"];
          console.log("getMassivePredictions : this.listPredictions = ", this.listPredictions, "listCorrections = ", this.listCorrections);
          console.log("before changeDetectorRef");
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
}
