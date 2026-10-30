import { useState } from "react";
import { Button, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { setServerUrl } from "./auth";

export default function ServerSetup() {
  const [url, setUrl] = useState("https://");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={{ padding: 24, gap: 16 }}>
        <Text style={{ fontSize: 26, fontWeight: "700" }}>OpenDuet</Text>
        <Text>
          Enter the URL of your OpenDuet / OpenBot server. The agents run on
          that server; this app connects to it.
        </Text>
        <TextInput
          accessibilityLabel="Server URL"
          placeholder="https://your-openduet-server.com"
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          value={url}
          onChangeText={setUrl}
          style={{ borderWidth: 1, borderRadius: 8, padding: 12 }}
        />
        {error ? <Text style={{ color: "#b42318" }}>{error}</Text> : null}
        <Button
          title={busy ? "Connecting..." : "Connect"}
          disabled={busy || url.length < 10}
          onPress={() => {
            setBusy(true);
            setError("");
            setServerUrl(url).catch((e) => {
              setError(e instanceof Error ? e.message : "Invalid URL");
              setBusy(false);
            });
          }}
        />
      </View>
    </SafeAreaView>
  );
}
