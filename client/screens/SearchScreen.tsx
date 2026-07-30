import { StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { mockProducts } from "../data/mockProducts";

export default function SearchScreen() {
	return (
		<SafeAreaView style={styles.container}>
			<View style={styles.topContent}>
				<Text style={{ fontSize: 25 }}>Welcome to Sorta</Text>

				<TextInput
					style={{
						marginTop: 20,
						backgroundColor: "#ffffff",
						width: "100%",
						height: 40,
						paddingHorizontal: 15,
						borderRadius: 20,
					}}
				>
					Placeholder
				</TextInput>
			</View>

			<View style={styles.mainContent}></View>
		</SafeAreaView>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#fff9e9",
		alignItems: "center",
		justifyContent: "center",
	},
	topContent: {
		width: "90%",
		marginTop: 50,
		paddingHorizontal: 20,
		alignItems: "center",
		justifyContent: "flex-start",
	},
	mainContent: {
		flex: 1,
		marginTop: 20,
		width: "90%",
		paddingHorizontal: 20,
		alignItems: "center",
		justifyContent: "flex-start",
		backgroundColor: "#ffffff",
	},
});
