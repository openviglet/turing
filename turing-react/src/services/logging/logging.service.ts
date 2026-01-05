import type { TurLoggingGeneral } from "@/models/logging/logging-general.model";
import type { TurLoggingIndexing } from "@/models/logging/logging-indexing.model";
import axios from "axios";

export class TurLoggingInstanceService {
  async server(): Promise<TurLoggingGeneral[]> {
    const response = await axios.get<TurLoggingGeneral[]>("/logging");
    return response.data;
  }
  async aem(): Promise<TurLoggingGeneral[]> {
    const response = await axios.get<TurLoggingGeneral[]>("/logging/aem");
    return response.data;
  }
  async indexing(): Promise<TurLoggingIndexing[]> {
    const response = await axios.get<TurLoggingIndexing[]>("/logging/indexing");
    return response.data;
  }
}
