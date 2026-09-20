-- =====================================================================
-- ELORA / PINKY DASHBOARD - SUPABASE RLS AUDIT & SECURITY MIGRATION (JSONB - FINAL AUDITED)
-- =====================================================================
-- Description: 
-- This SQL migration establishes a robust, highly secure, and recursion-free 
-- Row Level Security (RLS) model for the Pinky/Elora Dashboard.
-- It strictly respects Owner-level privileges, granular active staff profile
-- permissions (stored as JSONB), and secure customer-level constraints.
--
-- Running Instructions:
-- Copy the entire contents of this file, paste it into your Supabase 
-- SQL Editor, and run it. It is safe, repeatable (idempotent), and clean.
-- =====================================================================

-- -----------------------------------------------------------------
-- 1. DATABASE SECURITY FUNCTIONS
-- -----------------------------------------------------------------

-- Helper: Check if current user is an active staff profile or the Owner.
-- Bypasses recursion by executing with SECURITY DEFINER privileges.
-- search_path is set to empty ('') to prevent hijacking attacks, requiring
-- fully schema-qualified identifiers.
CREATE OR REPLACE FUNCTION public.is_elora_staff()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_email pg_catalog.text;
    v_is_staff boolean;
BEGIN
    -- Get current authenticated user ID
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN false;
    END IF;

    -- Check if Owner by ID
    IF v_user_id = '50fefcea-5eab-44bd-80f9-9675648c6ec7'::uuid THEN
        RETURN true;
    END IF;

    -- Check if Owner by JWT Email
    v_email := auth.jwt() ->> 'email';
    IF v_email = 'ilnemrawy@gmail.com' THEN
        RETURN true;
    END IF;

    -- Check if active staff profile exists in the database
    SELECT EXISTS (
        SELECT 1 
        FROM public.staff_profiles 
        WHERE id = v_user_id 
          AND active = true
    ) INTO v_is_staff;

    RETURN v_is_staff;
END;
$$;

-- Helper: Check if current user has a specific granular staff permission.
-- Bypasses recursion by executing with SECURITY DEFINER privileges.
-- This function processes the "permissions" column as a JSONB object,
-- checking granular keys (e.g., 'orders.view', 'orders.manage').
-- It strictly avoids fuzzy mapping that grants manage rights from a view-only permission.
-- search_path is set to empty ('') to prevent hijacking attacks.
CREATE OR REPLACE FUNCTION public.has_staff_permission(required_permission pg_catalog.text)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_email pg_catalog.text;
    v_role pg_catalog.text;
    v_permissions jsonb;
    v_active boolean;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN false;
    END IF;

    -- Check if Owner by ID
    IF v_user_id = '50fefcea-5eab-44bd-80f9-9675648c6ec7'::uuid THEN
        RETURN true;
    END IF;

    -- Check if Owner by JWT Email
    v_email := auth.jwt() ->> 'email';
    IF v_email = 'ilnemrawy@gmail.com' THEN
        RETURN true;
    END IF;

    -- Fetch staff profile details from the database
    SELECT role, permissions, active
    INTO v_role, v_permissions, v_active
    FROM public.staff_profiles
    WHERE id = v_user_id;

    -- If profile is inactive, deny all permissions
    IF v_active IS NOT TRUE THEN
        RETURN false;
    END IF;

    -- Owner role always gets full authorization
    IF pg_catalog.lower(v_role) = 'owner' THEN
        RETURN true;
    END IF;

    -- If no permissions JSONB exists, deny
    IF v_permissions IS NULL THEN
        RETURN false;
    END IF;

    -- 1. Direct JSONB key check (e.g., 'orders.view', 'orders.manage', 'staff.manage', etc.)
    IF (v_permissions ->> required_permission) = 'true' THEN
        RETURN true;
    END IF;

    -- 2. Strict mapping for VIEW to MANAGE logic:
    -- If a view permission is requested, but they have the manage permission, allow it.
    IF required_permission = 'orders.view' AND (v_permissions ->> 'orders.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'products.view' AND (v_permissions ->> 'products.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'offers.view' AND (v_permissions ->> 'offers.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'categories.view' AND (v_permissions ->> 'categories.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'coupons.view' AND (v_permissions ->> 'coupons.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'shipping.view' AND (v_permissions ->> 'shipping.manage') = 'true' THEN
        RETURN true;
    END IF;

    IF required_permission = 'website.view' AND (v_permissions ->> 'website.manage') = 'true' THEN
        RETURN true;
    END IF;

    -- 3. Strict, non-leaking translation for Kotlin system permission constants:
    
    -- MANAGE_STAFF -> staff.manage
    IF required_permission = 'MANAGE_STAFF' THEN
        RETURN (v_permissions ->> 'staff.manage') = 'true';
    END IF;

    -- MANAGE_ORDERS -> orders.manage
    IF required_permission = 'MANAGE_ORDERS' THEN
        RETURN (v_permissions ->> 'orders.manage') = 'true';
    END IF;

    -- VIEW_ORDERS -> orders.view or orders.manage
    IF required_permission = 'VIEW_ORDERS' THEN
        RETURN (v_permissions ->> 'orders.view') = 'true' OR (v_permissions ->> 'orders.manage') = 'true';
    END IF;

    -- MANAGE_PRODUCTS -> products.manage
    IF required_permission = 'MANAGE_PRODUCTS' THEN
        RETURN (v_permissions ->> 'products.manage') = 'true';
    END IF;

    -- VIEW_PRODUCTS -> products.view or products.manage
    IF required_permission = 'VIEW_PRODUCTS' THEN
        RETURN (v_permissions ->> 'products.view') = 'true' OR (v_permissions ->> 'products.manage') = 'true';
    END IF;

    -- MANAGE_OFFERS -> offers.manage
    IF required_permission = 'MANAGE_OFFERS' THEN
        RETURN (v_permissions ->> 'offers.manage') = 'true';
    END IF;

    -- VIEW_OFFERS -> offers.view or offers.manage
    IF required_permission = 'VIEW_OFFERS' THEN
        RETURN (v_permissions ->> 'offers.view') = 'true' OR (v_permissions ->> 'offers.manage') = 'true';
    END IF;

    -- MANAGE_SETTINGS -> website.manage OR shipping.manage (Kotlin system-level request)
    IF required_permission = 'MANAGE_SETTINGS' THEN
        RETURN (v_permissions ->> 'website.manage') = 'true' OR (v_permissions ->> 'shipping.manage') = 'true';
    END IF;

    -- VIEW_SETTINGS -> shipping.view OR shipping.manage OR website.manage
    IF required_permission = 'VIEW_SETTINGS' THEN
        RETURN (v_permissions ->> 'shipping.view') = 'true' OR (v_permissions ->> 'shipping.manage') = 'true' OR (v_permissions ->> 'website.manage') = 'true';
    END IF;

    RETURN false;
END;
$$;


-- -----------------------------------------------------------------
-- 2. ENABLE ROW LEVEL SECURITY (RLS) ON ALL TARGET TABLES
-- -----------------------------------------------------------------
ALTER TABLE IF EXISTS public.staff_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.banners ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.shipping_governorates ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.shipping_centers ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.prime_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.site_customizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.store_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.dashboard_push_devices ENABLE ROW LEVEL SECURITY;


-- -----------------------------------------------------------------
-- 3. DROP ALL EXISTING POLICIES TO PREVENT CONFLICTS (Idempotency)
-- -----------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT policyname, tablename 
        FROM pg_policies 
        WHERE schemaname = 'public' 
          AND tablename IN (
              'staff_profiles', 'orders', 'products', 'categories', 
              'offers', 'banners', 'coupons', 'shipping_governorates', 
              'shipping_centers', 'prime_subscriptions', 
              'site_customizations', 'store_settings', 'dashboard_push_devices'
          )
    ) LOOP
        EXECUTE 'DROP POLICY IF EXISTS ' || quote_ident(r.policyname) || ' ON ' || quote_ident(r.tablename);
    END LOOP;
END;
$$;


-- -----------------------------------------------------------------
-- 4. TABLE-SPECIFIC POLICY IMPLEMENTATIONS
-- -----------------------------------------------------------------

-- ==========================================
-- TABLE: staff_profiles
-- ==========================================
-- Select: Staff can view all profiles; users can view their own profile.
CREATE POLICY select_staff_profiles ON public.staff_profiles
    FOR SELECT TO authenticated
    USING (auth.uid() = id OR public.has_staff_permission('staff.manage'));

-- Insert/Update/Delete: Only users with staff.manage permission can modify profiles.
CREATE POLICY manage_staff_profiles ON public.staff_profiles
    FOR ALL TO authenticated
    USING (public.has_staff_permission('staff.manage'))
    WITH CHECK (public.has_staff_permission('staff.manage'));


-- ==========================================
-- TABLE: orders
-- ==========================================
-- Select: Staff with orders.view or orders.manage permissions can view all; customers can view only their own orders.
CREATE POLICY select_orders ON public.orders
    FOR SELECT
    USING (
        public.has_staff_permission('orders.view') OR public.has_staff_permission('orders.manage')
        OR 
        (customer_user_id IS NOT NULL AND customer_user_id::text = auth.uid()::text)
    );

-- Insert: Anonymous store visitors and registered users can create orders (checkout process).
CREATE POLICY insert_orders ON public.orders
    FOR INSERT
    WITH CHECK (true);

-- Update: Staff with orders.manage permission can update orders. Customers cannot update their own orders.
CREATE POLICY update_orders ON public.orders
    FOR UPDATE TO authenticated
    USING (
        public.has_staff_permission('orders.manage')
    );

-- Delete: Only authorized staff with orders.manage permission can delete orders. Customers cannot delete.
CREATE POLICY delete_orders ON public.orders
    FOR DELETE TO authenticated
    USING (
        public.has_staff_permission('orders.manage')
    );


-- ==========================================
-- TABLE: products
-- ==========================================
-- Select: Strictly NO public/anonymous/customer access to public.products to protect sensitive margins.
-- Only staff with products.view or products.manage permissions can SELECT from public.products.
-- General public reads active products through the catalog_products view.
CREATE POLICY select_products ON public.products
    FOR SELECT TO authenticated
    USING (
        public.has_staff_permission('products.view') OR public.has_staff_permission('products.manage')
    );

-- Write operations: Only authorized staff with products.manage permission can modify.
CREATE POLICY modify_products ON public.products
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('products.manage')
    )
    WITH CHECK (
        public.has_staff_permission('products.manage')
    );


-- ==========================================
-- TABLE: categories
-- ==========================================
-- Select: Anyone can view active categories; staff with categories.view or categories.manage can view all.
CREATE POLICY select_categories ON public.categories
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('categories.view') OR public.has_staff_permission('categories.manage'))
    );

-- Write operations: Only staff with categories.manage permission can modify.
CREATE POLICY modify_categories ON public.categories
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('categories.manage')
    )
    WITH CHECK (
        public.has_staff_permission('categories.manage')
    );


-- ==========================================
-- TABLE: offers
-- ==========================================
-- Select: Anyone can view active offers; staff with offers.view or offers.manage can view all.
CREATE POLICY select_offers ON public.offers
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('offers.view') OR public.has_staff_permission('offers.manage'))
    );

-- Write operations: Only staff with offers.manage permission can modify.
CREATE POLICY modify_offers ON public.offers
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('offers.manage')
    )
    WITH CHECK (
        public.has_staff_permission('offers.manage')
    );


-- ==========================================
-- TABLE: banners
-- ==========================================
-- Select: Anyone can view active banners; staff with offers.view or offers.manage can view all.
CREATE POLICY select_banners ON public.banners
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('offers.view') OR public.has_staff_permission('offers.manage'))
    );

-- Write operations: Only staff with offers.manage permission can modify.
CREATE POLICY modify_banners ON public.banners
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('offers.manage')
    )
    WITH CHECK (
        public.has_staff_permission('offers.manage')
    );


-- ==========================================
-- TABLE: coupons
-- ==========================================
-- Select: Anyone can read active coupons to allow checkout verification; staff with coupons.view or coupons.manage can view all.
CREATE POLICY select_coupons ON public.coupons
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('coupons.view') OR public.has_staff_permission('coupons.manage'))
    );

-- Write operations: Only authorized staff with coupons.manage permission can modify.
CREATE POLICY modify_coupons ON public.coupons
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('coupons.manage')
    )
    WITH CHECK (
        public.has_staff_permission('coupons.manage')
    );


-- ==========================================
-- TABLE: shipping_governorates
-- ==========================================
-- Select: Anyone can view active governorates; staff with shipping.view or shipping.manage can view all.
CREATE POLICY select_govs ON public.shipping_governorates
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('shipping.view') OR public.has_staff_permission('shipping.manage'))
    );

-- Write operations: Only authorized staff with shipping.manage can modify.
CREATE POLICY modify_govs ON public.shipping_governorates
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('shipping.manage')
    )
    WITH CHECK (
        public.has_staff_permission('shipping.manage')
    );


-- ==========================================
-- TABLE: shipping_centers
-- ==========================================
-- Select: Anyone can view active shipping centers; staff with shipping.view or shipping.manage can view all.
CREATE POLICY select_centers ON public.shipping_centers
    FOR SELECT
    USING (
        active = true 
        OR (public.has_staff_permission('shipping.view') OR public.has_staff_permission('shipping.manage'))
    );

-- Write operations: Only authorized staff with shipping.manage can modify.
CREATE POLICY modify_centers ON public.shipping_centers
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('shipping.manage')
    )
    WITH CHECK (
        public.has_staff_permission('shipping.manage')
    );


-- ==========================================
-- TABLE: prime_subscriptions
-- ==========================================
-- Select: Staff with shipping.view/manage, website.manage, or orders.view/manage can view all. Clients can view only their own subscription. Exact UUID check.
CREATE POLICY select_prime ON public.prime_subscriptions
    FOR SELECT
    USING (
        public.has_staff_permission('shipping.view') 
        OR public.has_staff_permission('shipping.manage') 
        OR public.has_staff_permission('website.manage') 
        OR public.has_staff_permission('orders.view') 
        OR public.has_staff_permission('orders.manage')
        OR (user_id IS NOT NULL AND auth.uid() = user_id)
    );

-- Write operations: Only staff with shipping.manage or website.manage can manage subscriptions.
CREATE POLICY modify_prime ON public.prime_subscriptions
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('shipping.manage') 
        OR public.has_staff_permission('website.manage')
    )
    WITH CHECK (
        public.has_staff_permission('shipping.manage') 
        OR public.has_staff_permission('website.manage')
    );


-- ==========================================
-- TABLE: site_customizations
-- ==========================================
-- Select: Public access allowed for displaying the website frontend.
CREATE POLICY select_site_cust ON public.site_customizations
    FOR SELECT
    USING (true);

-- Write operations: Only active staff with website.manage permission can modify.
CREATE POLICY modify_site_cust ON public.site_customizations
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('website.manage')
    )
    WITH CHECK (
        public.has_staff_permission('website.manage')
    );


-- ==========================================
-- TABLE: store_settings
-- ==========================================
-- Select: Public access allowed for frontend store settings.
CREATE POLICY select_store_settings ON public.store_settings
    FOR SELECT
    USING (true);

-- Write operations: Only active staff with website.manage permission can modify store settings.
CREATE POLICY modify_store_settings ON public.store_settings
    FOR ALL TO authenticated
    USING (
        public.has_staff_permission('website.manage')
    )
    WITH CHECK (
        public.has_staff_permission('website.manage')
    );


-- ==========================================
-- TABLE: dashboard_push_devices
-- ==========================================
-- Select: Users can view their own tokens; staff with staff.manage can view all. Exact UUID verification.
CREATE POLICY select_push ON public.dashboard_push_devices
    FOR SELECT TO authenticated
    USING (user_id = auth.uid() OR public.has_staff_permission('staff.manage'));

-- Insert: Users can register their own device tokens. Exact UUID check.
CREATE POLICY insert_push ON public.dashboard_push_devices
    FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

-- Update: Users can update their own tokens; staff with staff.manage can update all.
CREATE POLICY update_push ON public.dashboard_push_devices
    FOR UPDATE TO authenticated
    USING (user_id = auth.uid() OR public.has_staff_permission('staff.manage'));

-- Delete: Users can delete their own tokens; staff with staff.manage can delete all.
CREATE POLICY delete_push ON public.dashboard_push_devices
    FOR DELETE TO authenticated
    USING (user_id = auth.uid() OR public.has_staff_permission('staff.manage'));

-- =====================================================================
-- Migration complete. Copy/paste this in the Supabase SQL Editor and run.
-- =====================================================================
