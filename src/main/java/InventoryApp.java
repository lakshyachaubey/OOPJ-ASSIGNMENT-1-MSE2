import java.sql.*;
import java.util.Scanner;

public class InventoryApp {
    private static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    private static final String USER = "root";
    private static final String PASS = "root"; // ⚠️ Change to your MySQL password!

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Scanner sc = new Scanner(System.in)) {

            String createTable = "CREATE TABLE IF NOT EXISTS Product (" +
                    "product_id INT PRIMARY KEY, " +
                    "product_name VARCHAR(100), " +
                    "category VARCHAR(50), " +
                    "quantity INT, " +
                    "price DOUBLE)";
            conn.createStatement().execute(createTable);

            while (true) {
                System.out.println("\n--- Inventory Management System ---");
                System.out.println("1. Insert Products (a)");
                System.out.println("2. Retrieve by Category & Qty < 20 (b)");
                System.out.println("3. Update Stock (c)");
                System.out.println("4. Update Price (d)");
                System.out.println("5. Total Value & Delete (e)");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1: insertProducts(conn); break;
                    case 2: retrieveAndSort(conn, sc); break;
                    case 3: updateStock(conn, sc); break;
                    case 4: updatePrice(conn, sc); break;
                    case 5: aggregateAndDelete(conn, sc); break;
                    case 6: return;
                    default: System.out.println("Invalid choice.");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // (a) Insert Records & Handle Duplicates
    private static void insertProducts(Connection conn) {
        String sql = "INSERT INTO Product VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Object[][] data = {
                    {101, "Laptop", "Electronics", 15, 60000.0},
                    {102, "Mouse", "Electronics", 50, 500.0},
                    {103, "Chair", "Furniture", 10, 3000.0}
            };
            for (Object[] row : data) {
                pstmt.setInt(1, (int) row[0]);
                pstmt.setString(2, (String) row[1]);
                pstmt.setString(3, (String) row[2]);
                pstmt.setInt(4, (int) row[3]);
                pstmt.setDouble(5, (double) row[4]);
                try {
                    pstmt.executeUpdate();
                    System.out.println("✅ Inserted: " + row[1]);
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("❌ Duplicate ID skipped: " + row[0]);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // (b) Conditional Retrieval and Sorting
    private static void retrieveAndSort(Connection conn, Scanner sc) {
        System.out.print("Enter Category (e.g., Electronics): ");
        String category = sc.next();
        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getInt("product_id") + " | " + rs.getString("product_name") +
                        " | Qty: " + rs.getInt("quantity") + " | Price: " + rs.getDouble("price"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // (c) Update Stock
    private static void updateStock(Connection conn, Scanner sc) {
        System.out.print("Enter Product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter Quantity to Add: ");
        int qty = sc.nextInt();

        String sql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, qty);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) System.out.println("✅ Stock updated successfully.");
            else System.out.println("❌ Product does not exist.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // (d) Search and Modify Price
    private static void updatePrice(Connection conn, Scanner sc) {
        System.out.print("Enter Product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter New Price: ");
        double price = sc.nextDouble();

        if (price <= 0) {
            System.out.println("❌ Price must be greater than zero.");
            return;
        }

        String sql = "UPDATE Product SET price = ? WHERE product_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, price);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Price updated. Fetching details:");
                PreparedStatement check = conn.prepareStatement("SELECT * FROM Product WHERE product_id = ?");
                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    System.out.println(rs.getString("product_name") + " -> New Price: " + rs.getDouble("price"));
                }
            } else System.out.println("❌ Product not found.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // (e) Aggregate Query and Conditional Deletion
    private static void aggregateAndDelete(Connection conn, Scanner sc) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT SUM(quantity * price) FROM Product");
            if (rs.next()) System.out.println("💰 Total Inventory Value: " + rs.getDouble(1));

            System.out.print("Enter Product ID to delete: ");
            int id = sc.nextInt();

            String delSql = "DELETE FROM Product WHERE product_id = ? AND quantity = 0";
            try (PreparedStatement pstmt = conn.prepareStatement(delSql)) {
                pstmt.setInt(1, id);
                int rows = pstmt.executeUpdate();
                if (rows > 0) System.out.println("✅ Product deleted successfully.");
                else System.out.println("❌ Cannot delete: Product not found or quantity is not zero.");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
