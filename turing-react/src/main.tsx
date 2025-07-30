import { BrowserRouter } from 'react-router-dom'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import React from 'react'
import axios from 'axios'
import { environment } from './environment/environment.ts'

axios.defaults.auth = {
  username: "admin",
  password: "turing",
};
axios.defaults.baseURL = `${environment.apiUrl}/api`;

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
