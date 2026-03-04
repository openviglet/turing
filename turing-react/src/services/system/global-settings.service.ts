import type {
  TurGlobalDecimalSeparator,
  TurGlobalSettings,
} from "@/models/system/global-settings.model";
import axios from "axios";

export class TurGlobalSettingsService {
  async query(): Promise<TurGlobalSettings> {
    const response = await axios.get<TurGlobalSettings>(
      "/system/global-settings",
    );
    return response.data;
  }

  async update(
    decimalSeparator: TurGlobalDecimalSeparator,
  ): Promise<TurGlobalSettings> {
    const response = await axios.put<TurGlobalSettings>(
      "/system/global-settings",
      {
        decimalSeparator,
      },
    );
    return response.data;
  }
}
