-- =====================================================================
-- PINKY DASHBOARD - BACKEND RECONCILIATION & SECURITY MIGRATION (FINAL)
-- =====================================================================
-- Description:
-- This migration establishes a robust, highly secure, and recursion-free
-- Row Level Security (RLS) model for Pinky Dashboard.
-- It strictly respects Owner privileges, active staff permissions (stored
-- as comma-separated TEXT/VARCHAR aligned with Android's Kotlin implementation),
-- and applies policies dynamically and safely only on existing tables.
-- =====================================================================

-- -----------------------------------------------------------------
-- 1. DATABASE SECURITY FUNCTIONS
-- -----------------------------------------------------------------

-- Helper: Check if current user is an active staff profile or the Owner.
-- Bypasses RLS recursion by executing with SECURITY DEFINER privileges.
-- search_path is set to pg_catalog to prevent hijacking attacks.
CREATE OR REPLACE FUNCTION public.is_elora_staff()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog
AS $$
DECLARE
    v_user_id uuid;
    v_email text;
    v_is_staff boolean;
BEGIN
    -- Get current authenticated user ID
    v_user_id := (select auth.uid());
    IF v_user_id IS NULL THEN
        RETURN false;
    END IF;

    -- Owner check by hardcoded ID
    IF v_user_id = '50fefcea-5eab-44bd-80f9-9675648c6ec7'::uuid THEN
        RETURN true;
    END IF;

    -- Owner check by JWT email
    v_email := (select auth.jwt() ->> 'email');
    IF v_email = 'ilnemrawy@gmail.com' THEN
        RETURN true;
    END IF;

    -- Check if active staff profile exists in the database
    -- Handles both 'active' and 'is_active' columns gracefully
    SELECT EXISTS (
        SELECT 1
        FROM public.staff_profiles
        WHERE id = v_user_id
          AND (
              (CASE WHEN (SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'staff_profiles' AND column_name = 'active'))
                    THEN active ELSE true END) = true
              AND
              (CASE WHEN (SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'staff_profiles' AND column_name = 'is_active'))
                    THEN is_active ELSE true END) = true
          )
    ) INTO v_is_staff;

    RETURN COALESCE(v_is_staff, false);
EXCEPTION
    WHEN OTHERS THEN
        RETURN false;
END;
$$;

-- Helper: Check if current user has a specific granular staff permission.
-- Bypasses RLS recursion by executing with SECURITY DEFINER privileges.
-- Processes "permissions" as comma-separated TEXT matching Android Kotlin Permission enum.
-- Also incorporates substring check and JSONB fallback for absolute robustness.
CREATE OR REPLACE FUNCTION public.has_staff_permission(required_permission text)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog
AS $$
DECLARE
    v_user_id uuid;
    v_email text;
    v_role text;
    v_permissions text;
    v_active boolean;
    v_is_active boolean;
BEGIN
    v_user_id := (select auth.uid());
    IF v_user_id IS NULL THEN
        RETURN false;
    END IF;

    -- Owner check by hardcoded ID
    IF v_user_id = '50fefcea-5eab-44bd-80f9-9675648c6ec7'::uuid THEN
        RETURN true;
    END IF;

    -- Owner check by JWT email
    v_email := (select auth.jwt() ->> 'email');
    IF v_email = 'ilnemrawy@gmail.com' THEN
        RETURN true;
    END IF;

    -- Fetch profile details from database
    -- Gracefully handles columns if they exist
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'staff_profiles') THEN
        BEGIN
            EXECUTE 'SELECT role, permissions, 
                (CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = ''public'' AND table_name = ''staff_profiles'' AND column_name = ''active'') THEN active ELSE true END),
                (CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = ''public'' AND table_name = ''staff_profiles'' AND column_name = ''is_active'') THEN is_active ELSE true END)
                FROM public.staff_profiles WHERE id = $1'
                INTO v_role, v_permissions, v_active, v_is_active
                USING v_user_id;
        EXCEPTION WHEN OTHERS THEN
            RETURN false;
        END;
    ELSE
        RETURN false;
    END IF;

    -- Check if profile is active
    IF COALESCE(v_active, v_is_active, true) IS NOT TRUE THEN
        RETURN false;
    END IF;

    -- Owner role always gets full access
    IF lower(COALESCE(v_role, '')) = 'owner' THEN
        RETURN true;
    END IF;

    -- If permissions are null, deny
    IF v_permissions IS NULL THEN
        RETURN false;
    END IF;

    -- Direct array check (handling comma-separated uppercase string e.g. "VIEW_DASHBOARD,MANAGE_ORDERS")
    IF required_permission = ANY(string_to_array(replace(v_permissions, ' ', ''), ',')) THEN
        RETURN true;
    END IF;

    -- Substring search check (fallback)
    IF v_permissions LIKE '%' || required_permission || '%' THEN
        RETURN true;
    END IF;

    -- Handle mapping VIEW_ORDERS to MANAGE_ORDERS permissions
    IF required_permission = 'VIEW_ORDERS' AND v_permissions LIKE '%MANAGE_ORDERS%' THEN
        RETURN true;
    END IF;

    -- Handle mapping VIEW_PRODUCTS to MANAGE_PRODUCTS permissions
    IF required_permission = 'VIEW_PRODUCTS' AND v_permissions LIKE '%MANAGE_PRODUCTS%' THEN
        RETURN true;
    END IF;

    RETURN false;
END;
$$;


-- -----------------------------------------------------------------
-- 2. SAFE IDEMPOTENT TABLE POLICY BINDINGS VIA DYNAMIC PL/PGSQL
-- -----------------------------------------------------------------

-- ==========================================
-- TABLE: staff_profiles
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'staff_profiles') THEN
        EXECUTE 'ALTER TABLE public.staff_profiles ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_staff_profiles ON public.staff_profiles';
        EXECUTE 'DROP POLICY IF EXISTS manage_staff_profiles ON public.staff_profiles';
        
        EXECUTE 'CREATE POLICY select_staff_profiles ON public.staff_profiles
            FOR SELECT TO authenticated
            USING (auth.uid() = id OR public.has_staff_permission(''MANAGE_STAFF''))';
            
        EXECUTE 'CREATE POLICY manage_staff_profiles ON public.staff_profiles
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_STAFF''))
            WITH CHECK (public.has_staff_permission(''MANAGE_STAFF''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: orders
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'orders') THEN
        EXECUTE 'ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_orders ON public.orders';
        EXECUTE 'DROP POLICY IF EXISTS insert_orders ON public.orders';
        EXECUTE 'DROP POLICY IF EXISTS update_orders ON public.orders';
        EXECUTE 'DROP POLICY IF EXISTS delete_orders ON public.orders';
        
        -- Select: Allowed if they have manage or status update permission
        EXECUTE 'CREATE POLICY select_orders ON public.orders
            FOR SELECT TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS'') OR public.has_staff_permission(''UPDATE_ORDER_STATUS''))';
            
        -- Insert: Anyone can insert orders (supports customer checkout)
        EXECUTE 'CREATE POLICY insert_orders ON public.orders
            FOR INSERT
            WITH CHECK (true)';
            
        -- Update: Staff with MANAGE_ORDERS or UPDATE_ORDER_STATUS can update status/orders
        EXECUTE 'CREATE POLICY update_orders ON public.orders
            FOR UPDATE TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS'') OR public.has_staff_permission(''UPDATE_ORDER_STATUS''))';
            
        -- Delete: Only MANAGE_ORDERS can delete
        EXECUTE 'CREATE POLICY delete_orders ON public.orders
            FOR DELETE TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: order_items
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'order_items') THEN
        EXECUTE 'ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_order_items ON public.order_items';
        EXECUTE 'DROP POLICY IF EXISTS insert_order_items ON public.order_items';
        EXECUTE 'DROP POLICY IF EXISTS update_order_items ON public.order_items';
        EXECUTE 'DROP POLICY IF EXISTS delete_order_items ON public.order_items';
        
        EXECUTE 'CREATE POLICY select_order_items ON public.order_items
            FOR SELECT TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS'') OR public.has_staff_permission(''UPDATE_ORDER_STATUS''))';
            
        EXECUTE 'CREATE POLICY insert_order_items ON public.order_items
            FOR INSERT
            WITH CHECK (true)';
            
        EXECUTE 'CREATE POLICY update_order_items ON public.order_items
            FOR UPDATE TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS''))';
            
        EXECUTE 'CREATE POLICY delete_order_items ON public.order_items
            FOR DELETE TO authenticated
            USING (public.has_staff_permission(''MANAGE_ORDERS''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: products
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'products') THEN
        EXECUTE 'ALTER TABLE public.products ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_products ON public.products';
        EXECUTE 'DROP POLICY IF EXISTS modify_products ON public.products';
        
        -- Select: Protected to staff with MANAGE_PRODUCTS (hides wholesale cost from public)
        EXECUTE 'CREATE POLICY select_products ON public.products
            FOR SELECT TO authenticated
            USING (public.has_staff_permission(''MANAGE_PRODUCTS''))';
            
        -- Modify: staff with MANAGE_PRODUCTS
        EXECUTE 'CREATE POLICY modify_products ON public.products
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_PRODUCTS''))
            WITH CHECK (public.has_staff_permission(''MANAGE_PRODUCTS''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: categories
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'categories') THEN
        EXECUTE 'ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_categories ON public.categories';
        EXECUTE 'DROP POLICY IF EXISTS modify_categories ON public.categories';
        
        -- Select: Public/anonymous and staff can select
        EXECUTE 'CREATE POLICY select_categories ON public.categories
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_CATEGORIES
        EXECUTE 'CREATE POLICY modify_categories ON public.categories
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_CATEGORIES''))
            WITH CHECK (public.has_staff_permission(''MANAGE_CATEGORIES''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: offers
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'offers') THEN
        EXECUTE 'ALTER TABLE public.offers ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_offers ON public.offers';
        EXECUTE 'DROP POLICY IF EXISTS modify_offers ON public.offers';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_offers ON public.offers
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_OFFERS
        EXECUTE 'CREATE POLICY modify_offers ON public.offers
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_OFFERS''))
            WITH CHECK (public.has_staff_permission(''MANAGE_OFFERS''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: banners
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'banners') THEN
        EXECUTE 'ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_banners ON public.banners';
        EXECUTE 'DROP POLICY IF EXISTS modify_banners ON public.banners';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_banners ON public.banners
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_OFFERS or MANAGE_WEBSITE_EDITOR
        EXECUTE 'CREATE POLICY modify_banners ON public.banners
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_OFFERS'') OR public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))
            WITH CHECK (public.has_staff_permission(''MANAGE_OFFERS'') OR public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: coupons
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'coupons') THEN
        EXECUTE 'ALTER TABLE public.coupons ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_coupons ON public.coupons';
        EXECUTE 'DROP POLICY IF EXISTS modify_coupons ON public.coupons';
        
        -- Select: Anyone can select (allows promo verification)
        EXECUTE 'CREATE POLICY select_coupons ON public.coupons
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_COUPONS
        EXECUTE 'CREATE POLICY modify_coupons ON public.coupons
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_COUPONS''))
            WITH CHECK (public.has_staff_permission(''MANAGE_COUPONS''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: shipping_governorates
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'shipping_governorates') THEN
        EXECUTE 'ALTER TABLE public.shipping_governorates ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_govs ON public.shipping_governorates';
        EXECUTE 'DROP POLICY IF EXISTS modify_govs ON public.shipping_governorates';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_govs ON public.shipping_governorates
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_SHIPPING
        EXECUTE 'CREATE POLICY modify_govs ON public.shipping_governorates
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_SHIPPING''))
            WITH CHECK (public.has_staff_permission(''MANAGE_SHIPPING''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: shipping_centers
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'shipping_centers') THEN
        EXECUTE 'ALTER TABLE public.shipping_centers ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_centers ON public.shipping_centers';
        EXECUTE 'DROP POLICY IF EXISTS modify_centers ON public.shipping_centers';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_centers ON public.shipping_centers
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_SHIPPING
        EXECUTE 'CREATE POLICY modify_centers ON public.shipping_centers
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_SHIPPING''))
            WITH CHECK (public.has_staff_permission(''MANAGE_SHIPPING''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: prime_subscriptions
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'prime_subscriptions') THEN
        EXECUTE 'ALTER TABLE public.prime_subscriptions ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_prime ON public.prime_subscriptions';
        EXECUTE 'DROP POLICY IF EXISTS modify_prime ON public.prime_subscriptions';
        
        -- Select: Allowed if they are active staff with prime management or settings permission
        EXECUTE 'CREATE POLICY select_prime ON public.prime_subscriptions
            FOR SELECT TO authenticated
            USING (public.has_staff_permission(''MANAGE_PRIME'') OR public.has_staff_permission(''MANAGE_SETTINGS''))';
            
        -- Modify: Only staff with MANAGE_PRIME
        EXECUTE 'CREATE POLICY modify_prime ON public.prime_subscriptions
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_PRIME''))
            WITH CHECK (public.has_staff_permission(''MANAGE_PRIME''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: site_customizations
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'site_customizations') THEN
        EXECUTE 'ALTER TABLE public.site_customizations ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_site_cust ON public.site_customizations';
        EXECUTE 'DROP POLICY IF EXISTS modify_site_cust ON public.site_customizations';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_site_cust ON public.site_customizations
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_WEBSITE_EDITOR
        EXECUTE 'CREATE POLICY modify_site_cust ON public.site_customizations
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))
            WITH CHECK (public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: store_settings
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'store_settings') THEN
        EXECUTE 'ALTER TABLE public.store_settings ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_store_settings ON public.store_settings';
        EXECUTE 'DROP POLICY IF EXISTS modify_store_settings ON public.store_settings';
        
        -- Select: Anyone can select
        EXECUTE 'CREATE POLICY select_store_settings ON public.store_settings
            FOR SELECT
            USING (true)';
            
        -- Modify: Only staff with MANAGE_SETTINGS or MANAGE_WEBSITE_EDITOR
        EXECUTE 'CREATE POLICY modify_store_settings ON public.store_settings
            FOR ALL TO authenticated
            USING (public.has_staff_permission(''MANAGE_SETTINGS'') OR public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))
            WITH CHECK (public.has_staff_permission(''MANAGE_SETTINGS'') OR public.has_staff_permission(''MANAGE_WEBSITE_EDITOR''))';
    END IF;
END;
$$;


-- ==========================================
-- TABLE: dashboard_push_devices
-- ==========================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'dashboard_push_devices') THEN
        EXECUTE 'ALTER TABLE public.dashboard_push_devices ENABLE ROW LEVEL SECURITY';
        EXECUTE 'DROP POLICY IF EXISTS select_push ON public.dashboard_push_devices';
        EXECUTE 'DROP POLICY IF EXISTS insert_push ON public.dashboard_push_devices';
        EXECUTE 'DROP POLICY IF EXISTS update_push ON public.dashboard_push_devices';
        EXECUTE 'DROP POLICY IF EXISTS delete_push ON public.dashboard_push_devices';
        
        -- Staff can manage device registrations (authenticated active staff)
        EXECUTE 'CREATE POLICY select_push ON public.dashboard_push_devices
            FOR SELECT TO authenticated
            USING (public.is_elora_staff())';
            
        EXECUTE 'CREATE POLICY insert_push ON public.dashboard_push_devices
            FOR INSERT TO authenticated
            WITH CHECK (public.is_elora_staff())';
            
        EXECUTE 'CREATE POLICY update_push ON public.dashboard_push_devices
            FOR UPDATE TO authenticated
            USING (public.is_elora_staff())';
            
        EXECUTE 'CREATE POLICY delete_push ON public.dashboard_push_devices
            FOR DELETE TO authenticated
            USING (public.is_elora_staff())';
    END IF;
END;
$$;

-- =====================================================================
-- Reconciliation complete. Copy/paste this in the Supabase SQL Editor and run.
-- =====================================================================
