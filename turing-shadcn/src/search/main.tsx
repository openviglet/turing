import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import SearchApp from './SearchApp.tsx'
import '../index.css'
import axios from 'axios'

axios.defaults.baseURL = `${import.meta.env.VITE_API_URL}`;

// Use basename only in production
const basename = import.meta.env.MODE === 'production' ? '/sn/templates' : '/';

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter basename={basename}>
      <SearchApp />
    </BrowserRouter>
  </React.StrictMode>
)
