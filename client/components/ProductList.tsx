import React, { useState } from "react";
import { ScrollView } from "react-native";
import ProductCard from "./ProductCard";
import { Product } from "../types";

type Props = {
	products: Product[];
};

export default function ProductList({ products }: Props) {
	const [selectedId, setSelectedId] = useState<number | null>(null);

	return (
		<ScrollView style={{ width: "100%" }}>
			{products.map((product) => (
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
