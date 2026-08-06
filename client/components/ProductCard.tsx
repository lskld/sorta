import { View, Text, StyleSheet, Pressable } from "react-native";
import { Product } from "../data/mockProducts";

type Props = {
	product: Product;
	isSelected: boolean;
	onPress: () => void;
};

export default function ProductCard({ product, isSelected, onPress }: Props) {
	return (
		<Pressable onPress={onPress}>
			<View style={[styles.card, isSelected && styles.selectedCard]}>
				<Text style={styles.text}>{product.name}</Text>
				<Text style={styles.text}>{product.category}</Text>
				<Text style={styles.text}>Units sold: {product.units_sold}</Text>
			</View>
		</Pressable>
	);
}

const styles = StyleSheet.create({
	card: {
		backgroundColor: "#ffffff",
		width: "100%",
		height: 80,
		marginVertical: 3,
		alignItems: "center",
		justifyContent: "center",
		borderRadius: 10,

		// Shadow for iOS
		shadowColor: "#000",
		shadowOffset: { width: 0, height: 1 },
		shadowOpacity: 0.1,
		shadowRadius: 2,
		// Shadow for Android
		elevation: 3,
	},
	selectedCard: {
		borderColor: "#007aff",
		borderWidth: 2,
	},
	text: {
		textAlign: "center",
	},
});
