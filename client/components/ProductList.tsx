import React from "react";
import { mockProducts } from "../data/mockProducts";
import { View, Text } from "react-native";

export default function ProductList() {
    return (
        <View>
            {mockProducts.map((product) => (
                <Text key={product.product_id}>{product.name}</Text>
            ))}
        </View>
    );
}