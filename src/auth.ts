import { expoClient } from "@better-auth/expo/client";
import { ssoClient } from "@better-auth/sso/client";
import { createAuthClient } from "better-auth/react";
import * as SecureStore from "expo-secure-store";

import { reloadAppAsync } from "expo";

// OpenDuet mobile: the server URL can be entered in the app (saved on the device) or baked in at build time.
const SERVER_KEY = "openduet_server_url";
function validate(raw: string): URL {
  const endpoint = new URL(raw.trim());
  if (endpoint.protocol !== "https:" && endpoint.protocol !== "http:")
    throw new Error("Use an https:// URL.");
  if (endpoint.username || endpoint.password || endpoint.search || endpoint.hash)
    throw new Error(
      "Use the public deployment URL without credentials or query parameters.",
    );
  return endpoint;
}
function initialEndpoint(): URL | null {
  const stored = (() => {
    try {
      return SecureStore.getItem(SERVER_KEY);
    } catch {
      return null;
    }
  })();
  for (const candidate of [stored, process.env.EXPO_PUBLIC_SERVER_URL]) {
    if (!candidate) continue;
    try {
      return validate(candidate);
    } catch {}
  }
  return null;
}
const resolved = initialEndpoint();
export const serverConfigured = resolved !== null;
const endpoint = resolved ?? new URL("https://openduet.invalid");
export async function setServerUrl(raw: string) {
  const url = validate(raw);
  await SecureStore.setItemAsync(SERVER_KEY, url.origin);
  await reloadAppAsync();
}
export async function clearServerUrl() {
  await SecureStore.deleteItemAsync(SERVER_KEY);
  await reloadAppAsync();
}
export const serverUrl = endpoint.origin;
export const authClient = createAuthClient({
  baseURL: serverUrl,
  plugins: [
    expoClient({
      scheme: "openbotmobile",
      storagePrefix: `openbot-${endpoint.host.replace(/[^a-zA-Z0-9_-]/g, "-")}`,
      storage: SecureStore,
    }),
    ssoClient(),
  ],
});
export async function signIn(provider: "google" | "microsoft" | "okta") {
  const result = await authClient.signIn.social({
    provider: provider as "google",
    callbackURL: "openbotmobile://",
  });
  if (result.error)
    throw new Error(result.error.message ?? "Could not sign in.");
}
export async function signInEnterprise(email: string) {
  const result = await authClient.signIn.sso({
    email,
    callbackURL: "openbotmobile://",
  });
  if (result.error)
    throw new Error(
      result.error.message ?? "Could not sign in to your organization.",
    );
}
