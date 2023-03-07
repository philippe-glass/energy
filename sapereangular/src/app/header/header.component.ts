import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit {

  constructor(private http: HttpClient, private router: Router) {

  }
  

  logout() {
    this.http.post('logout', {}).subscribe(() => {
        this.router.navigateByUrl('/');
    });
  }

  ngOnInit() {
  }

  

}
