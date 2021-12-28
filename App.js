import { StatusBar } from "expo-status-bar";
import { StyleSheet, Text, View, TouchableOpacity,NativeModules } from "react-native";
import * as DevMenu from "expo-dev-menu";


const HelloWorld = NativeModules.HelloWorldModule;
export default function App() {
  return (
    <View style={styles.container}>
      <Text>Open up App.js to start working on your app!</Text>
      <TouchableOpacity onPress={()=>{
        HelloWorld.ShowMessage("Hello World!",2000);
      }}>
        <Text>CLick</Text>

      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "center",
  },
  buttonContainer: {
    backgroundColor: "#4630eb",
    borderRadius: 4,
    padding: 12,
    marginVertical: 10,
    justifyContent: "center",
    alignItems: "center",
  },
  buttonText: {
    color: "#ffffff",
    fontSize: 16,
  },
});
