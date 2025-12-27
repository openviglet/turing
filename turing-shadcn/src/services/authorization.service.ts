import type { TurDiscoveryAPI } from "@/models/auth/discovery";
import type { TurRestInfo } from "@/models/auth/rest-info";
import axios, { type AxiosRequestConfig } from "axios";

export class TurAuthorizationService {
  async login(username: string, password: string): Promise<TurRestInfo> {
    const config: AxiosRequestConfig = {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Basic " + window.btoa(username + ":" + password),
      },
    };
    const response = await axios.create().get<TurRestInfo>("/v2", config);
    return response.data;
  }

  async discovery(): Promise<TurDiscoveryAPI> {
    const config: AxiosRequestConfig = {};
    const response = await axios
      .create()
      .get<TurDiscoveryAPI>("/discovery", config);
    return response.data;
  }

  logout() {
    localStorage.removeItem("restInfo");
  }
}
