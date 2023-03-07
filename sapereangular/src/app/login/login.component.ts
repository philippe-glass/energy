import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  username:String="";
  password:String="";
  

  constructor(private httpClient: HttpClient, private _constant: ConstantsService,private router: Router) { }

  ngOnInit() {
  }

  onSubmit(){
   // this.httpClient.post(this._constant.baseAppUrl+'user/checkuser',
     // { "username": this.username, "password": this.password} ,{ responseType: 'text' }).subscribe(res => {        
      //  if(res=="true")
          this.router.navigate(['/home']);
     // },
     // error=>{console.log("error:"+ error);
     // })
  }
}
