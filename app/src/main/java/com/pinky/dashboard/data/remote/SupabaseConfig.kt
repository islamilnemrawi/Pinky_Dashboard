package com.pinky.dashboard.data.remote

object SupabaseConfig {
    // Default Supabase configuration
    // Uses the public anon key for Row Level Security (RLS)
    // CRITICAL: NEVER include the service_role key in the Android APK
    const val DEFAULT_SUPABASE_URL = "https://dmttevcncsxzbamazmmq.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1waW5reSIsInJvbGUiOiJhbm9uIn0.placeholder"

    // Tables supporting backward compatibility with existing Elora schema
    const val TABLE_ORDERS = "orders"
    const val TABLE_ORDERS_LEGACY = "elora_orders"
    const val TABLE_ORDER_ITEMS = "order_items"
    const val TABLE_ORDER_ITEMS_LEGACY = "elora_order_items"
    const val TABLE_PRODUCTS = "products"
    const val TABLE_PRODUCTS_LEGACY = "elora_products"
    const val TABLE_CATEGORIES = "categories"
    const val TABLE_OFFERS = "offers"
    const val TABLE_CUSTOMERS = "customers"
    const val TABLE_SHIPPING = "shipping_rates"
    const val TABLE_ADMIN_PROFILES = "admin_profiles"

    // Cloud Storage Bucket for product images
    const val BUCKET_PRODUCT_IMAGES = "pinky-products"
    const val BUCKET_BANNERS = "pinky-banners"
}
