package com.pinky.dashboard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pinky.dashboard.data.local.dao.*
import com.pinky.dashboard.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        OfferEntity::class,
        CustomerEntity::class,
        ShippingGovernorateEntity::class,
        ShippingCenterEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PinkyDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun offerDao(): OfferDao
    abstract fun customerDao(): CustomerDao
    abstract fun shippingDao(): ShippingDao

    companion object {
        @Volatile
        private var INSTANCE: PinkyDatabase? = null

        fun getInstance(context: Context): PinkyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PinkyDatabase::class.java,
                    "pinky_dashboard.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                // Seeding of mock sandbox data is permanently disabled per production requirements.
                val isPlaceholder = false
                if (isPlaceholder) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            seedMockData(instance)
                        } catch (e: Exception) {
                            android.util.Log.e("PinkyDatabase", "Database seed failed: ${e.message}", e)
                        }
                    }
                }

                instance
            }
        }
    }
}

private suspend fun seedMockData(db: PinkyDatabase) {
    val categoryDao = db.categoryDao()
    val productDao = db.productDao()
    val orderDao = db.orderDao()
    val customerDao = db.customerDao()
    val shippingDao = db.shippingDao()
    val offerDao = db.offerDao()

    // 1. Check if database is already seeded
    if (productDao.getProductById("prod_1") != null) return

    // 2. Categories
    val categories = listOf(
        CategoryEntity("cat_abayas", "عبايات استقبال", "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400", 1, true, 4),
        CategoryEntity("cat_pajamas", "بيجامات بيتي", "https://images.unsplash.com/photo-1562572159-4ebcd318f4dd?w=400", 2, true, 3),
        CategoryEntity("cat_scarves", "طرح وإيشاربات", "https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=400", 3, true, 3),
        CategoryEntity("cat_accessories", "إكسسوارات", "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=400", 4, true, 2)
    )
    categoryDao.insertCategories(categories)

    // 3. Products
    val products = listOf(
        ProductEntity(
            id = "prod_1",
            name = "عباءة استقبال ملكية مطرزة بالخيوط الذهبية",
            description = "عباءة استقبال ملكية فاخرة بخامات عالية الجودة وتطريز يدوي ذهبي مميز ومريح للمناسبات والمنزل.",
            imageUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400",
            galleryJson = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400",
            categoryId = "cat_abayas",
            categoryName = "عبايات استقبال",
            price = 1250.0,
            oldPrice = 1500.0,
            discountPercent = 17,
            wholesalePrice = 850.0,
            internalCode = "ABY-001",
            stock = 15,
            colorsJson = "أسود,كحلي,مارون",
            sizesJson = "L,XL,XXL",
            isActive = true,
            isFeatured = true,
            createdAt = System.currentTimeMillis()
        ),
        ProductEntity(
            id = "prod_2",
            name = "بيجامة مخملية ناعمة قطعتين شتوية",
            description = "بيجامة شتوية ناعمة للغاية من قطعتين مع كود مرن لتوفير أقصى درجات الدفء والراحة المنزلية اليومية.",
            imageUrl = "https://images.unsplash.com/photo-1562572159-4ebcd318f4dd?w=400",
            galleryJson = "https://images.unsplash.com/photo-1562572159-4ebcd318f4dd?w=400",
            categoryId = "cat_pajamas",
            categoryName = "بيجامات بيتي",
            price = 650.0,
            oldPrice = 800.0,
            discountPercent = 18,
            wholesalePrice = 420.0,
            internalCode = "PJ-002",
            stock = 28,
            colorsJson = "وردي,رمادي,أزرق نيل",
            sizesJson = "M,L,XL",
            isActive = true,
            isFeatured = true,
            createdAt = System.currentTimeMillis() - 100000
        ),
        ProductEntity(
            id = "prod_3",
            name = "وشاح شيفون ليزر كويتي فاخر",
            description = "إيشارب شيفون ليزر مصنع من أفضل خيوط الحرير الكويتي المقاوم للانزلاق والمتناسق مع جميع الإطلالات.",
            imageUrl = "https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=400",
            galleryJson = "https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=400",
            categoryId = "cat_scarves",
            categoryName = "طرح وإيشاربات",
            price = 180.0,
            oldPrice = 220.0,
            discountPercent = 18,
            wholesalePrice = 110.0,
            internalCode = "SHF-003",
            stock = 50,
            colorsJson = "بيج,أوف وايت,وردي ناعم",
            sizesJson = "قاسي,مرن",
            isActive = true,
            isFeatured = false,
            createdAt = System.currentTimeMillis() - 200000
        ),
        ProductEntity(
            id = "prod_4",
            name = "سوار مطلي بالذهب عيار 18 من عزة فهمي",
            description = "سلسلة وأسورة نسائية كلاسيكية مطلية بماء الذهب عيار 18 بتصميم فريد مستوحى من التراث العربي الأصيل.",
            imageUrl = "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=400",
            galleryJson = "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=400",
            categoryId = "cat_accessories",
            categoryName = "إكسسوارات",
            price = 450.0,
            oldPrice = 600.0,
            discountPercent = 25,
            wholesalePrice = 280.0,
            internalCode = "ACC-004",
            stock = 10,
            colorsJson = "ذهبي,فضي",
            sizesJson = "قطعة واحدة",
            isActive = true,
            isFeatured = true,
            createdAt = System.currentTimeMillis() - 300000
        )
    )
    productDao.insertProducts(products)

    // 4. Customers
    val customers = listOf(
        CustomerEntity(
            id = "cust_1",
            name = "أروى إسلام النمراوي",
            phone = "01002345678",
            email = "arwa@pinky.eg",
            address = "شارع الميرغني، مصر الجديدة",
            governorate = "القاهرة",
            totalOrders = 3,
            totalSpend = 3150.0,
            firstOrderDate = System.currentTimeMillis() - 20 * 24 * 60 * 60 * 1000L,
            lastOrderDate = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L
        ),
        CustomerEntity(
            id = "cust_2",
            name = "شيماء محمد صبري",
            phone = "01123456789",
            email = "shaimaa@gmail.com",
            address = "سموحة بجوار نادي سموحة",
            governorate = "الإسكندرية",
            totalOrders = 1,
            totalSpend = 1250.0,
            firstOrderDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
            lastOrderDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L
        ),
        CustomerEntity(
            id = "cust_3",
            name = "ياسمين محمود البنداري",
            phone = "01234567890",
            email = "yasmin@outlook.com",
            address = "المنصورة شارع جيهان الرئيسي",
            governorate = "الدقهلية",
            totalOrders = 1,
            totalSpend = 650.0,
            firstOrderDate = System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000L,
            lastOrderDate = System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000L
        )
    )
    customerDao.insertCustomers(customers)

    // 5. Orders & Items
    val orders = listOf(
        OrderEntity(
            id = "ord_1",
            orderNumber = "PNK-2026-001",
            customerName = "أروى إسلام النمراوي",
            customerPhone = "01002345678",
            customerAddress = "شارع الميرغني، مصر الجديدة",
            governorate = "القاهرة",
            shippingCenter = "مكتب شحن القاهرة الرئيسي",
            subtotalAmount = 2500.0,
            shippingFee = 50.0,
            discountAmount = 200.0,
            totalAmount = 2350.0,
            paymentMethod = "البطاقات الائتمانية (Visa / MasterCard)",
            paymentStatus = "PAID",
            status = "تم التوصيل",
            createdAt = System.currentTimeMillis() - 15 * 24 * 60 * 60 * 1000L,
            notes = "التوصيل بعد الساعة 5 مساءً"
        ),
        OrderEntity(
            id = "ord_2",
            orderNumber = "PNK-2026-002",
            customerName = "شيماء محمد صبري",
            customerPhone = "01123456789",
            customerAddress = "سموحة بجوار نادي سموحة",
            governorate = "الإسكندرية",
            shippingCenter = "مكتب شحن الإسكندرية الرئيسي",
            subtotalAmount = 1250.0,
            shippingFee = 60.0,
            discountAmount = 0.0,
            totalAmount = 1310.0,
            paymentMethod = "فودافون كاش ومحافظ الهاتف",
            paymentStatus = "PAID",
            status = "تم الشحن",
            createdAt = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
            notes = "برجاء الاتصال قبل التوصيل بنصف ساعة"
        ),
        OrderEntity(
            id = "ord_3",
            orderNumber = "PNK-2026-003",
            customerName = "ياسمين محمود البنداري",
            customerPhone = "01234567890",
            customerAddress = "المنصورة شارع جيهان الرئيسي",
            governorate = "الدقهلية",
            shippingCenter = "مكتب شحن الدلتا المركزي",
            subtotalAmount = 650.0,
            shippingFee = 70.0,
            discountAmount = 50.0,
            totalAmount = 670.0,
            paymentMethod = "الدفع عند الاستلام (COD)",
            paymentStatus = "PENDING",
            status = "قيد الانتظار",
            createdAt = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
            notes = ""
        )
    )
    orderDao.insertOrders(orders)

    val orderItems = listOf(
        OrderItemEntity("ord_item_1_1", "ord_1", "prod_1", "عباءة استقبال ملكية مطرزة بالخيوط الذهبية", "أسود / XXL", "أسود", "XXL", 2, 1250.0, 2500.0, "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400"),
        OrderItemEntity("ord_item_2_1", "ord_2", "prod_1", "عباءة استقبال ملكية مطرزة بالخيوط الذهبية", "كحلي / XL", "كحلي", "XL", 1, 1250.0, 1250.0, "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400"),
        OrderItemEntity("ord_item_3_1", "ord_3", "prod_2", "بيجامة مخملية ناعمة قطعتين شتوية", "وردي / L", "وردي", "L", 1, 650.0, 650.0, "https://images.unsplash.com/photo-1562572159-4ebcd318f4dd?w=400")
    )
    orderDao.insertOrderItems(orderItems)

    // 6. Shipping Governorates
    val govs = listOf(
        ShippingGovernorateEntity("gov_1", "القاهرة", 50.0, "1-2 أيام", true),
        ShippingGovernorateEntity("gov_2", "الجيزة", 50.0, "1-2 أيام", true),
        ShippingGovernorateEntity("gov_3", "الإسكندرية", 60.0, "2-3 أيام", true),
        ShippingGovernorateEntity("gov_4", "الدقهلية", 70.0, "2-3 أيام", true),
        ShippingGovernorateEntity("gov_5", "الغربية", 70.0, "2-3 أيام", true)
    )
    shippingDao.insertGovernorates(govs)

    // 7. Shipping Centers
    val centers = listOf(
        ShippingCenterEntity("center_1", "gov_1", "مكتب شحن القاهرة الرئيسي", 50.0, "01009876543", true),
        ShippingCenterEntity("center_2", "gov_3", "مكتب شحن الإسكندرية الرئيسي", 60.0, "01119876543", true)
    )
    shippingDao.insertCenters(centers)

    // 8. Offers
    val offers = listOf(
        OfferEntity(
            id = "off_1",
            title = "خصم الجمعة البيضاء المميز",
            description = "احصل على خصم فوري يصل إلى 25% على قسم العبايات بالكامل والتوصيل مجاني لعملاء القاهرة والجيزة.",
            discountText = "خصم 25%",
            imageUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400",
            buttonText = "تسوق الآن",
            targetCategoryId = "cat_abayas",
            targetCategoryName = "عبايات استقبال",
            sortOrder = 1,
            isActive = true,
            startDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
            endDate = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L
        )
    )
    offerDao.insertOffers(offers)
}
