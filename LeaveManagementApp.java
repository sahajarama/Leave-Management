import java.sql.*;
import java.util.Scanner;

public class LeaveManagementApp {
    static final String DB_URL = "jdbc:mysql://localhost:3306/leave_management";
    static final String USER = "root";
    static final String PASS = "123456"; // replace with your MySQL password
    static Scanner sc = new Scanner(System.in);
    static int loggedInEmpId;
    static String loggedInRole;

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("===== Welcome to Leave Management System =====");

            if (login(conn)) {
                if (loggedInRole.equalsIgnoreCase("Admin")) {
                    adminMenu(conn);
                } else {
                    employeeMenu(conn);
                }
            } else {
                System.out.println("Login failed. Exiting...");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Employee/Admin Login
    private static boolean login(Connection conn) throws SQLException {
        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String password = sc.nextLine().trim();

        String sql = "SELECT emp_id, role FROM employees WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                loggedInEmpId = rs.getInt("emp_id");
                loggedInRole = rs.getString("role");
                System.out.println("Login successful! Role: " + loggedInRole);
                return true;
            }
        }
        return false;
    }

    // Employee Menu
    private static void employeeMenu(Connection conn) throws SQLException {
        while (true) {
            System.out.println("\n1. Apply Leave");
            System.out.println("2. View My Leaves");
            System.out.println("3. View Leave Balance");
            System.out.println("4. Logout");
            System.out.print("Enter choice: ");
            int choice = getValidInt();

            switch (choice) {
                case 1 -> applyLeave(conn);
                case 2 -> viewLeaves(conn, loggedInEmpId);
                case 3 -> viewLeaveBalance(conn, loggedInEmpId);
                case 4 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    // Admin Menu
    private static void adminMenu(Connection conn) throws SQLException {
        while (true) {
            System.out.println("\n1. View All Leaves");
            System.out.println("2. Approve/Reject Leave");
            System.out.println("3. View Pending Leaves");
            System.out.println("4. Logout");
            System.out.print("Enter choice: ");
            int choice = getValidInt();

            switch (choice) {
                case 1 -> viewAllLeaves(conn);
                case 2 -> approveLeave(conn);
                case 3 -> viewPendingLeaves(conn);
                case 4 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    // Apply Leave
    private static void applyLeave(Connection conn) throws SQLException {
        System.out.print("Enter Leave Type (Sick/Casual/Earned): ");
        String type = sc.nextLine().trim();

        System.out.print("Enter Start Date (YYYY-MM-DD): ");
        String startDate = sc.nextLine().trim();

        System.out.print("Enter End Date (YYYY-MM-DD): ");
        String endDate = sc.nextLine().trim();

        // Check leave balance
        String balanceSql = "SELECT " + type.toLowerCase() + "_leave FROM employees WHERE emp_id=?";
        try (PreparedStatement ps = conn.prepareStatement(balanceSql)) {
            ps.setInt(1, loggedInEmpId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int balance = rs.getInt(1);
                if (balance <= 0) {
                    System.out.println("Insufficient leave balance for " + type + " leave.");
                    return;
                }
            }
        }

        String sql = "INSERT INTO leaves (emp_id, leave_type, start_date, end_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loggedInEmpId);
            ps.setString(2, type);
            ps.setString(3, startDate);
            ps.setString(4, endDate);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Leave applied successfully!");
        }
    }

    // View My Leaves
    private static void viewLeaves(Connection conn, int empId) throws SQLException {
        String sql = "SELECT * FROM leaves WHERE emp_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            System.out.println("\nLeave ID | Type | Start | End | Status");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s\n",
                        rs.getInt("leave_id"),
                        rs.getString("leave_type"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getString("status"));
            }
        }
    }

    // View Leave Balance
    private static void viewLeaveBalance(Connection conn, int empId) throws SQLException {
        String sql = "SELECT sick_leave, casual_leave, earned_leave FROM employees WHERE emp_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Sick Leave: " + rs.getInt("sick_leave"));
                System.out.println("Casual Leave: " + rs.getInt("casual_leave"));
                System.out.println("Earned Leave: " + rs.getInt("earned_leave"));
            }
        }
    }

    // Approve or Reject Leave
    private static void approveLeave(Connection conn) throws SQLException {
        System.out.print("Enter Leave ID: ");
        int leaveId = getValidInt();
        System.out.print("Enter Status (Approved/Rejected): ");
        String status = sc.nextLine().trim();

        // Update leave table
        String updateSql = "UPDATE leaves SET status=? WHERE leave_id=?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, status);
            ps.setInt(2, leaveId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Leave status updated!");
                if (status.equalsIgnoreCase("Approved")) {
                    // Deduct leave balance
                    String leaveTypeSql = "SELECT emp_id, leave_type FROM leaves WHERE leave_id=?";
                    try (PreparedStatement ps2 = conn.prepareStatement(leaveTypeSql)) {
                        ps2.setInt(1, leaveId);
                        ResultSet rs = ps2.executeQuery();
                        if (rs.next()) {
                            int empId = rs.getInt("emp_id");
                            String type = rs.getString("leave_type").toLowerCase();
                            String deductSql = "UPDATE employees SET " + type + "_leave = " + type + "_leave - 1 WHERE emp_id=?";
                            try (PreparedStatement ps3 = conn.prepareStatement(deductSql)) {
                                ps3.setInt(1, empId);
                                ps3.executeUpdate();
                            }
                        }
                    }
                }
            } else {
                System.out.println("Leave ID not found!");
            }
        }
    }

    // View All Leaves (Admin)
    private static void viewAllLeaves(Connection conn) throws SQLException {
        String sql = "SELECT * FROM leaves";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("Leave ID | Emp ID | Type | Start | End | Status");
            while (rs.next()) {
                System.out.printf("%d | %d | %s | %s | %s | %s\n",
                        rs.getInt("leave_id"),
                        rs.getInt("emp_id"),
                        rs.getString("leave_type"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getString("status"));
            }
        }
    }

    // View Pending Leaves (Admin)
    private static void viewPendingLeaves(Connection conn) throws SQLException {
        String sql = "SELECT * FROM leaves WHERE status='Pending'";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("Leave ID | Emp ID | Type | Start | End | Status");
            while (rs.next()) {
                System.out.printf("%d | %d | %s | %s | %s | %s\n",
                        rs.getInt("leave_id"),
                        rs.getInt("emp_id"),
                        rs.getString("leave_type"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getString("status"));
            }
        }
    }

    // Utility: Get valid integer input
    private static int getValidInt() {
        while (!sc.hasNextInt()) {
            System.out.print("Enter a valid number: ");
            sc.next();
        }
        int value = sc.nextInt();
        sc.nextLine(); // consume newline
        return value;
    }
}
