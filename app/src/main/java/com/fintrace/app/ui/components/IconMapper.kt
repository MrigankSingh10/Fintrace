package com.fintrace.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    val availableIcons = listOf(
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "LocalMall" to Icons.Default.LocalMall,
        "TrendingUp" to Icons.Default.TrendingUp,
        "LocalHospital" to Icons.Default.LocalHospital,
        "MoreHoriz" to Icons.Default.MoreHoriz,
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "Smartphone" to Icons.Default.Smartphone,
        "Payments" to Icons.Default.Payments,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Fastfood" to Icons.Default.Fastfood,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Home" to Icons.Default.Home,
        "ElectricBolt" to Icons.Default.ElectricBolt,
        "Flight" to Icons.Default.Flight,
        "School" to Icons.Default.School,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Tv" to Icons.Default.Tv,
        "Pets" to Icons.Default.Pets,
        "Category" to Icons.Default.Category
    )

    fun getIcon(name: String?): ImageVector {
        return availableIcons.find { it.first.equals(name, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }
}
