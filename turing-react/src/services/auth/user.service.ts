import type { TurUser } from "@/models/auth/user";
import axios from "axios";

export class TurUserService {
  async get(): Promise<TurUser> {
    const response = await axios.get<TurUser>(`/v2/user/current`);
    return response.data;
  }
}
