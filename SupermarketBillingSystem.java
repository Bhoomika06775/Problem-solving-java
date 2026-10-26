import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task 12: Supermarket Billing System (Use Case 2)
 * Concepts: Point-of-Sale (POS) Engine, Product Catalog, Inventory Management,
 *           Discount/Coupon Rule Engine, Tax Breakdown (GST), Itemized Receipt Generation,
 *           and Daily Sales Auditing.
 */
public class SupermarketBillingSystem {

    public enum ProductCategory {
        GROCERY(0.05),     // 5% GST
        DAIRY(0.05),       // 5% GST
        BEVERAGES(0.12),   // 12% GST
        ELECTRONICS(0.18), // 18% GST
        PERSONAL_CARE(0.18);

        private final double taxRate;
        ProductCategory(double taxRate) { this.taxRate = taxRate; }
        public double getTaxRate() { return taxRate; }
    }

    // Product Entity
    public static class Product {
        private final String sku;
        private final String name;
        private final ProductCategory category;
        private final double unitPrice;
        private int stockQuantity;
        private final boolean isWeighted; // Sold by Kg or Piece

        public Product(String sku, String name, ProductCategory category, double unitPrice, int stockQuantity, boolean isWeighted) {
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.unitPrice = unitPrice;
            this.stockQuantity = stockQuantity;
            this.isWeighted = isWeighted;
        }

        public String getSku() { return sku; }
        public String getName() { return name; }
        public ProductCategory getCategory() { return category; }
        public double getUnitPrice() { return unitPrice; }
        public int getStockQuantity() { return stockQuantity; }
        public boolean isWeighted() { return isWeighted; }

        public void reduceStock(int qty) {
            this.stockQuantity -= qty;
        }

        @Override
        public String toString() {
            return String.format("[%s] %-18s | %-12s | $%-6.2f | Stock: %d",
                    sku, name, category, unitPrice, stockQuantity);
        }
    }

    // Cart Line Item
    public static class CartItem {
        private final Product product;
        private final double quantityOrWeight;
        private double itemDiscount = 0.0;

        public CartItem(Product product, double quantityOrWeight) {
            this.product = product;
            this.quantityOrWeight = quantityOrWeight;
        }

        public Product getProduct() { return product; }
        public double getQuantityOrWeight() { return quantityOrWeight; }
        public double getItemDiscount() { return itemDiscount; }
        public void setItemDiscount(double discount) { this.itemDiscount = discount; }

        public double getBaseTotal() {
            return product.getUnitPrice() * quantityOrWeight;
        }

        public double getNetTotal() {
            return Math.max(0, getBaseTotal() - itemDiscount);
        }

        public double getTaxAmount() {
            return getNetTotal() * product.getCategory().getTaxRate();
        }
    }

    // Customer Shopping Cart
    public static class ShoppingCart {
        private final String cartId;
        private final String customerName;
        private final List<CartItem> items = new ArrayList<>();
        private String appliedCoupon = null;
        private double couponDiscount = 0.0;

        public ShoppingCart(String cartId, String customerName) {
            this.cartId = cartId;
            this.customerName = customerName;
        }

        public void addItem(Product product, double quantityOrWeight) {
            if (product.getStockQuantity() < (int) Math.ceil(quantityOrWeight)) {
                System.out.printf("  [OUT OF STOCK] Cannot add %.2f of %s (Only %d left)%n",
                        quantityOrWeight, product.getName(), product.getStockQuantity());
                return;
            }
            items.add(new CartItem(product, quantityOrWeight));
            System.out.printf("  [CART ADDED] %-18s x %-5.2f @ $%.2f%n",
                    product.getName(), quantityOrWeight, product.getUnitPrice());
        }

        public void applyCoupon(String couponCode) {
            if ("MEGA10".equalsIgnoreCase(couponCode)) {
                this.appliedCoupon = "MEGA10";
                double sub = getSubtotal();
                if (sub >= 50.0) {
                    this.couponDiscount = sub * 0.10; // 10% storewide coupon
                    System.out.printf("  [COUPON APPLIED] 'MEGA10' saved $%.2f on your order!%n", couponDiscount);
                } else {
                    System.out.println("  [COUPON REJECTED] 'MEGA10' requires minimum spend of $50.00.");
                }
            } else {
                System.out.println("  [COUPON ERROR] Invalid promo code: " + couponCode);
            }
        }

        public double getSubtotal() {
            return items.stream().mapToDouble(CartItem::getBaseTotal).sum();
        }

        public double getItemDiscountsTotal() {
            return items.stream().mapToDouble(CartItem::getItemDiscount).sum();
        }

        public double getCouponDiscount() {
            return couponDiscount;
        }

        public double getTotalTax() {
            return items.stream().mapToDouble(CartItem::getTaxAmount).sum();
        }

        public double getGrandTotal() {
            return getSubtotal() - getItemDiscountsTotal() - couponDiscount + getTotalTax();
        }

        public List<CartItem> getItems() { return items; }
        public String getCustomerName() { return customerName; }
        public String getCartId() { return cartId; }
    }

    // POS Supermarket Engine
    public static class SupermarketPOS {
        private final String storeName;
        private final Map<String, Product> inventory = new HashMap<>();
        private double totalDailySales = 0.0;
        private int totalInvoicesIssued = 0;

        public SupermarketPOS(String storeName) {
            this.storeName = storeName;
        }

        public void addProduct(Product p) {
            inventory.put(p.getSku(), p);
        }

        public Product getProduct(String sku) {
            return inventory.get(sku);
        }

        public void checkout(ShoppingCart cart, String paymentMethod, double cashTendered) {
            System.out.println("\n=========================================================================================");
            System.out.printf("                           %s                           %n", storeName.toUpperCase());
            System.out.println("                       TAX INVOICE & CASH RECEIPT                                ");
            System.out.println("=========================================================================================");
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
            String invoiceNo = "INV-" + (10000 + (++totalInvoicesIssued));

            System.out.printf("Invoice No : %-20s Date: %s%n", invoiceNo, dateStr);
            System.out.printf("Customer   : %-20s Cashier: POS-Station-04%n", cart.getCustomerName());
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("%-6s %-18s %-6s %-8s %-8s %-8s %-8s%n",
                    "SKU", "Item Description", "Qty", "Rate", "Total", "Tax", "Net");
            System.out.println("-----------------------------------------------------------------------------------------");

            for (CartItem item : cart.getItems()) {
                Product p = item.getProduct();
                double lineTotal = item.getBaseTotal();
                double tax = item.getTaxAmount();
                double net = item.getNetTotal() + tax;

                System.out.printf("%-6s %-18s %-6.2f $%-7.2f $%-7.2f $%-7.2f $%-7.2f%n",
                        p.getSku(), p.getName(), item.getQuantityOrWeight(),
                        p.getUnitPrice(), lineTotal, tax, net);

                // Deduct Inventory Stock
                p.reduceStock((int) Math.ceil(item.getQuantityOrWeight()));
            }

            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("%-42s : $%,8.2f%n", "Gross Subtotal", cart.getSubtotal());
            if (cart.getItemDiscountsTotal() > 0) {
                System.out.printf("%-42s : -$%,7.2f%n", "Item Level Promotions", cart.getItemDiscountsTotal());
            }
            if (cart.getCouponDiscount() > 0) {
                System.out.printf("%-42s : -$%,7.2f (Promo: %s)%n",
                        "Storewide Coupon Discount", cart.getCouponDiscount(), cart.appliedCoupon);
            }
            System.out.printf("%-42s : $%,8.2f%n", "Applicable GST / Sales Tax", cart.getTotalTax());
            System.out.println("-----------------------------------------------------------------------------------------");
            double grandTotal = cart.getGrandTotal();
            System.out.printf("%-42s : $%,8.2f%n", "FINAL AMOUNT PAYABLE", grandTotal);
            System.out.println("=========================================================================================");

            // Payment reconciliation
            System.out.printf("Payment Method    : %s%n", paymentMethod);
            if ("CASH".equalsIgnoreCase(paymentMethod)) {
                double change = cashTendered - grandTotal;
                System.out.printf("Amount Tendered   : $%.2f%n", cashTendered);
                System.out.printf("Change Returned   : $%.2f%n", Math.max(0, change));
            } else {
                System.out.println("Transaction Status: APPROVED (Auth Code: 894120)");
            }

            totalDailySales += grandTotal;
            System.out.println("\n  *** Thank You for Shopping with Us! Please Visit Again! ***\n");
        }

        public void printInventoryStatus() {
            System.out.println("=========================================================================================");
            System.out.println("                   CURRENT INVENTORY STOCK AUDIT                                         ");
            System.out.println("=========================================================================================");
            inventory.values().stream()
                    .sorted(Comparator.comparing(Product::getCategory).thenComparing(Product::getName))
                    .forEach(System.out::println);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("Total Invoices Generated : %d%n", totalInvoicesIssued);
            System.out.printf("Total Turnover Today     : $%,.2f%n", totalDailySales);
            System.out.println("=========================================================================================\n");
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            SUPERMARKET BILLING & INVENTORY MANAGEMENT SYSTEM (USE CASE 2)               ");
        System.out.println("=========================================================================================");

        SupermarketPOS pos = new SupermarketPOS("FreshMart Super Center");

        // 1. Stock Catalog Setup
        pos.addProduct(new Product("SKU101", "Almond Milk 1L",      ProductCategory.DAIRY,         3.50, 45, false));
        pos.addProduct(new Product("SKU102", "Greek Yogurt 500g",   ProductCategory.DAIRY,         4.20, 30, false));
        pos.addProduct(new Product("SKU201", "Organic Honey Crisp", ProductCategory.GROCERY,       2.80, 80, true)); // per kg
        pos.addProduct(new Product("SKU202", "Basmati Rice 5kg",    ProductCategory.GROCERY,      14.50, 25, false));
        pos.addProduct(new Product("SKU301", "Arabica Cold Brew",   ProductCategory.BEVERAGES,     4.00, 50, false));
        pos.addProduct(new Product("SKU401", "Wireless Earbuds",    ProductCategory.ELECTRONICS,  29.99, 15, false));
        pos.addProduct(new Product("SKU501", "Herbal Shampoo",      ProductCategory.PERSONAL_CARE, 7.50, 40, false));

        System.out.println("\n--- Step 1: Initial Store Catalog & Stock Levels ---");
        pos.printInventoryStatus();

        // 2. Customer Cart 1: General Grocery & Produce with Mega Coupon
        System.out.println("--- Step 2: Processing Customer Cart 1 (Mr. Robert Langdon) ---");
        ShoppingCart cart1 = new ShoppingCart("CART-01", "Robert Langdon");
        cart1.addItem(pos.getProduct("SKU101"), 2);       // 2x Milk
        cart1.addItem(pos.getProduct("SKU201"), 3.5);     // 3.5 kg Apples
        cart1.addItem(pos.getProduct("SKU202"), 1);       // 1x Rice
        cart1.addItem(pos.getProduct("SKU401"), 1);       // 1x Earbuds
        cart1.applyCoupon("MEGA10");

        // Checkout Cart 1 (Cash Payment)
        pos.checkout(cart1, "CASH", 70.00);

        // 3. Customer Cart 2: Quick Essentials (Card Payment)
        System.out.println("--- Step 3: Processing Customer Cart 2 (Dr. Eleanor Vance) ---");
        ShoppingCart cart2 = new ShoppingCart("CART-02", "Dr. Eleanor Vance");
        cart2.addItem(pos.getProduct("SKU102"), 2);
        cart2.addItem(pos.getProduct("SKU301"), 3);
        cart2.addItem(pos.getProduct("SKU501"), 1);

        // Checkout Cart 2 (UPI/Card)
        pos.checkout(cart2, "CREDIT_CARD", cart2.getGrandTotal());

        // 4. End of Day Inventory & Turnover Report
        System.out.println("--- Step 4: End-of-Day Store Audit Report ---");
        pos.printInventoryStatus();
    }
}
