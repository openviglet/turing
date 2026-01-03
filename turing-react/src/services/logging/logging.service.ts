import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import axios from "axios";

export class TurLoggingInstanceService {
  async query(): Promise<TurLoggingGeneral[]> {
    const response = await axios.get<TurLoggingGeneral[]>("/logging");
    return response.data;
  }
}
