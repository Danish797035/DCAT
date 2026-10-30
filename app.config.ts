import type { ExpoConfig } from "expo/config";

const projectId = process.env.EXPO_PUBLIC_EAS_PROJECT_ID;
const config: ExpoConfig = {
  name: "OpenDuet",
  slug: "openduet",
  version: "0.0.1",
  scheme: "openbotmobile",
  ios: { bundleIdentifier: "ai.openbot.mobile", supportsTablet: true },
  android: { package: "com.danish.openduet", usesCleartextTraffic: true } as ExpoConfig["android"],
  plugins: [
    "expo-router",
    "expo-secure-store",
    ["expo-notifications", { defaultChannel: "openbot" }],
  ],
  extra: { ...(projectId ? { eas: { projectId } } : {}) },
};
export default config;
