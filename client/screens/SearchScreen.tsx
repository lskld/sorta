import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import ProductList from "../components/ProductList";
import { useState } from "react";
import { Platform } from "react-native";
import { Product } from "../types";

const getApiBaseUrl = () => {
	if (Platform.OS === "android") {
		return "http://10.0.2.2:8080";
	}

	return "http://localhost:8080";
};

export default function SearchScreen() {
	const [searchText, setSearchText] = useState("");
	const [products, setProducts] = useState<Product[]>([]);
	const [selectedId, setSelectedId] = useState<string | null>(null);

	const handleSearch = async () => {
		try {
			const response = await fetch(`${getApiBaseUrl()}/query`, {
				method: "POST",
				headers: {
					"Content-Type": "application/json",
				},
				body: JSON.stringify({
					query_text: searchText,
					anchor_id: selectedId,
				}),
			});
			const { results: apiProducts } = await response.json();
			setProducts(apiProducts as Product[]);
		} catch (error) {
			console.error("Search error:", error);
		}
	};

	return (
		<SafeAreaView style={styles.container}>
			<View style={styles.topContent}>
				<Text style={{ fontSize: 25 }}>Welcome to Sorta</Text>

				<View style={styles.searchRow}>
					<TextInput
						style={styles.searchInput}
						placeholder="Search..."
						value={searchText}
						onChangeText={setSearchText}
					/>
					<Pressable
						style={({ pressed }) => [
							styles.searchButton,
							{ transform: [{ scale: pressed ? 0.95 : 1 }] },
						]}
						onPress={handleSearch}
					>
						<Text style={{ color: "white", fontSize: 18 }}>Search</Text>
					</Pressable>
				</View>
			</View>

			<View style={styles.mainContent}>
				<ProductList
					products={products}
					selectedId={selectedId}
					setSelectedId={setSelectedId}
				/>
			</View>
		</SafeAreaView>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#efffef",
		alignItems: "center",
		justifyContent: "center",
	},
	topContent: {
		width: "90%",
		marginTop: 30,
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
	},
	searchRow: {
		flexDirection: "row",
		alignItems: "center",
		width: "100%",
		marginTop: 20,
	},
	searchInput: {
		flex: 1,
		backgroundColor: "#ffffff",
		height: 40,
		paddingHorizontal: 15,
		borderRadius: 20,
		shadowColor: "#000",
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.1,
		shadowRadius: 2,
		elevation: 3,
	},
	searchButton: {
		marginLeft: 10,
		backgroundColor: "#53e053",
		width: 80,
		height: 40,
		borderRadius: 20,
		justifyContent: "center",
		alignItems: "center",
	},
});
