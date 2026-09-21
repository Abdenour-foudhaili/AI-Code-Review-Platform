import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Topnav } from '../topnav/topnav';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Topnav],
  template: `
    <div class="shell-container flex h-screen w-full">
      <app-sidebar></app-sidebar>
      <div class="main-content flex-col w-full">
        <app-topnav></app-topnav>
        <main class="content-area">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell-container {
      background-color: var(--bg-main);
      overflow: hidden;
    }
    .main-content {
      flex: 1;
      height: 100vh;
      overflow: hidden;
      display: flex;
    }
    .content-area {
      flex: 1;
      overflow-y: auto;
      padding: var(--spacing-lg);
    }
  `]
})
export class ShellComponent {}
