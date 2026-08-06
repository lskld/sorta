import { View, Text, StyleSheet } from "react-native"
import { Product } from "../data/mockProducts"

export default function ProductCard({ product }: {
    product: Product
}) {
    return (
        <View style={styles.card}>
                <Text style={styles.text}>{product.name}</Text>
                <Text style={styles.text}>{product.category}</Text>
                <Text style={styles.text}>Units sold: {product.units_sold}</Text>
        </View>
    )
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: '#ffffff',
        width: '100%',
        height: 80,
        marginVertical: 3,
        alignItems: 'center',
        justifyContent: 'center'
    },
    text: {
        textAlign: 'center',
    }
})