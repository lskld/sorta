import React, { useState } from "react";
import { mockProducts } from "../data/mockProducts";
import { ScrollView } from "react-native";
import ProductCard from "./ProductCard";

export default function ProductList() {
	const [selectedId, setSelectedId] = useState<number | null>(null);

	return (
		<ScrollView style={{ width: "100%" }}>
			{mockProducts.map((product) => (
				<ProductCard
					product={product}
					key={product.product_id}
					isSelected={selectedId === product.product_id}
					onPress={() => setSelectedId(product.product_id)}
				/>
			))}
		</ScrollView>
	);
}
