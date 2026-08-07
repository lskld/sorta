import React from "react";
import { ScrollView } from "react-native";
import ProductCard from "./ProductCard";
import { Product } from "../types";

type Props = {
	products: Product[];
	selectedId: string | null;
	setSelectedId: (id: string | null) => void;
};

export default function ProductList({
	products,
	selectedId,
	setSelectedId,
}: Props) {
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
