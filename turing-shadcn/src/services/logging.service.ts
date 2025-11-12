import axios from "axios";
import type { TurLoggingInstance } from "@/models/logging/logging-instance.model";

export class TurLoggingInstanceService {
  async query(): Promise<TurLoggingInstance[]> {
    const response = await axios.get<TurLoggingInstance[]>("/logging");
    return response.data;
  }
  async get(id: string): Promise<TurLoggingInstance> {
    const response = await axios.get<TurLoggingInstance>(`/logging/${id}`);
    return response.data;
  }
  async create(turLoggingInstance: TurLoggingInstance): Promise<TurLoggingInstance> {
    const response = await axios.post<TurLoggingInstance>("/logging",
      turLoggingInstance
    );
    return response.data;
  }
  async update(turLoggingInstance: TurLoggingInstance): Promise<TurLoggingInstance> {
    const response = await axios.put<TurLoggingInstance>(
      `/logging/${turLoggingInstance.id.toString()}`,
      turLoggingInstance
    );
    return response.data;
  }
  async delete(turLoggingInstance: TurLoggingInstance): Promise<boolean> {
    const response = await axios.delete<TurLoggingInstance>(
      `/logging/${turLoggingInstance.id.toString()}`
    );
    return  response.status == 200;
  }
}
