import React from "react";
import { mockProducts } from "../data/mockProducts";
import { ScrollView } from "react-native";
import ProductCard from "./ProductCard";

export default function ProductList() {
	return (
		<ScrollView style={{ width: "100%" }}>
			{mockProducts.map((product) => (
				<ProductCard product={product} key={product.product_id} />
			))}
		</ScrollView>
	);
}
