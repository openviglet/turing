import axios from 'axios'
import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '../index.css'
import SearchApp from './SearchApp.tsx'

axios.defaults.baseURL = `${import.meta.env.VITE_API_URL}`;

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <SearchApp />
    </BrowserRouter>
  </React.StrictMode>
)
