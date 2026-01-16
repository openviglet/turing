import axios from "axios";

// Function to get CSRF token from cookies
function getCsrfToken(): string | null {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

// Configure axios defaults
axios.defaults.withCredentials = true;

// Add request interceptor to include CSRF token
axios.interceptors.request.use(
  (config) => {
    const csrfToken = getCsrfToken();
    if (
      csrfToken &&
      ["post", "put", "delete", "patch"].includes(
        config.method?.toLowerCase() || "",
      )
    ) {
      config.headers["X-XSRF-TOKEN"] = csrfToken;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// Add response interceptor to handle errors
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized - redirect to login if not already on login page
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);

export default axios;
