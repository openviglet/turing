import { BrowserRouter } from 'react-router-dom'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import React from 'react'
import axios from 'axios'
import { environment } from './environment/environment.ts'
import type { TurRestInfo } from './models/auth/rest-info.ts'

axios.defaults.baseURL = `${environment.apiUrl}/api`;
axios.interceptors.request.use((config) => {
  const token: TurRestInfo = getAuthToken();
  config.headers['Content-Type'] = 'application/json';
  config.headers['X-Requested-With'] = 'XMLHttpRequest';

  if (token.authdata) {
    config.headers.Authorization = `Basic ${token.authdata}`;
  }
  else {
    window.location.href = '/login?returnUrl=/admin';
  }
  return config;
});

const getAuthToken = (): TurRestInfo => {
  return JSON.parse(localStorage.getItem('restInfo') || "{}");
};

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
