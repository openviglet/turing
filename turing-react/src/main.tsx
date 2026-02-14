import '@/lib/axios'; // Configure axios interceptors for CSRF
import axios from 'axios';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.tsx';
import { ROUTES } from './app/routes.const.ts';
import './index.css';
import type { TurDiscoveryAPI } from './models/auth/discovery.ts';
import type { TurRestInfo } from './models/auth/rest-info.ts';
import { TurAuthorizationService } from './services/auth/authorization.service.ts';

const authorization = new TurAuthorizationService()
axios.defaults.baseURL = `${import.meta.env.VITE_API_URL}/api`;

// Cache discovery result to avoid repeated API calls
let discoveryCache: TurDiscoveryAPI | null = null;
let discoveryPromise: Promise<TurDiscoveryAPI> | null = null;

const getDiscovery = async (): Promise<TurDiscoveryAPI> => {
  if (discoveryCache) {
    return discoveryCache;
  }
  if (discoveryPromise) {
    return discoveryPromise;
  }
  discoveryPromise = authorization.discovery().then(result => {
    discoveryCache = result;
    discoveryPromise = null;
    return result;
  });
  return discoveryPromise;
};


axios.interceptors.request.use(async (config) => {
  config.headers['Content-Type'] = 'application/json';
  config.headers['X-Requested-With'] = 'XMLHttpRequest';
  try {
    const discovery = await getDiscovery();
    if (!discovery.keycloak) {
      const token: TurRestInfo = getAuthToken();
      if (token.authdata) {
        config.headers.Authorization = `Basic ${token.authdata}`;
      } else {
        globalThis.location.href = `${ROUTES.LOGIN}?returnUrl=${ROUTES.CONSOLE}`;
      }
    }
  } catch (error) {
    console.error('Error during request interception:', error);
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
