package com.tgw.stock.domain

/** Base measurement units. "base unit" for a product is what stock levels are tracked in. */
enum class StockUnit(val label: String) {
    KG("kg"),
    G("g"),
    L("L"),
    ML("ml"),
    PIECE("piece"),
    BOX("box"),
    PACK("pack"),
    TRAY("tray"),
    BOTTLE("bottle"),
    BAG("bag");

    companion object {
        fun fromNameSafe(name: String?): StockUnit =
            entries.firstOrNull { it.name == name } ?: PIECE
    }
}

enum class MovementType { IN, OUT, ADJUSTMENT }

enum class StockInReason(val label: String) {
    SUPPLIER_DELIVERY("Supplier Delivery"),
    STOCK_CORRECTION("Stock Correction"),
    RETURN("Return"),
    OTHER("Other")
}

enum class StockOutReason(val label: String) {
    KITCHEN_USAGE("Kitchen Usage"),
    WASTE("Waste"),
    EXPIRED("Expired"),
    STAFF_MEAL("Staff Meal"),
    DAMAGE("Damage"),
    STOCK_CORRECTION("Stock Correction"),
    OTHER("Other")
}

enum class WasteReason(val label: String) {
    EXPIRED("Expired"),
    DAMAGE("Damage"),
    OVERPRODUCTION("Overproduction"),
    SPOILAGE("Spoilage"),
    OTHER("Other")
}

enum class PurchaseOrderStatus(val label: String) {
    DRAFT("Draft"),
    ORDERED("Ordered"),
    PARTIALLY_DELIVERED("Partially Delivered"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

enum class StockCountStatus { DRAFT, COMPLETED }

enum class StockStatus { NORMAL, LOW, CRITICAL, OUT_OF_STOCK }
