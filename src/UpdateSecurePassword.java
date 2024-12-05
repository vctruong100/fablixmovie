//import java.sql.*;
//import java.util.ArrayList;
//
//import org.jasypt.util.password.PasswordEncryptor;
//import org.jasypt.util.password.StrongPasswordEncryptor;
//
//public class UpdateSecurePassword {
//
//    /*
//     *
//     * This program updates your existing moviedb customers table to change the
//     * plain text passwords to encrypted passwords.
//     *
//     * You should only run this program **once**, because this program uses the
//     * existing passwords as real passwords, then replace them. If you run it more
//     * than once, it will treat the encrypted passwords as real passwords and
//     * generate wrong values.
//     *
//     */
//    public static void main(String[] args) throws Exception {
//
//        String loginUser = "mytestuser";
//        String loginPasswd = "My6$Password";
//        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
//
//        Class.forName("com.mysql.jdbc.Driver").newInstance();
//        Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
//        Statement statement = connection.createStatement();
//
//        // change the customers table password column from VARCHAR(20) to VARCHAR(128)
//        try (PreparedStatement alterCustomerStmt = connection.prepareStatement(
//                "ALTER TABLE customers MODIFY COLUMN password VARCHAR(128)")) {
//            int alterResult = alterCustomerStmt.executeUpdate();
//            System.out.println("altering customers table schema completed, " + alterResult + " rows affected");
//        }
//
//        // Update employees table
//        try (PreparedStatement alterEmployeeStmt = connection.prepareStatement(
//                "ALTER TABLE employees MODIFY COLUMN password VARCHAR(128)")) {
//            int alterEmployeeResult = alterEmployeeStmt.executeUpdate();
//            System.out.println("altering employees table schema completed, " + alterEmployeeResult + " rows affected");
//        }
//
//        // get the ID and password for each customer
//        String query = "SELECT id, password FROM customers";
//        PreparedStatement selectCustomer = connection.prepareStatement(query);
//        ResultSet rs = selectCustomer.executeQuery();
//
//        // we use the StrongPasswordEncryptor from jasypt library (Java Simplified Encryption)
//        //  it internally use SHA-256 algorithm and 10,000 iterations to calculate the encrypted password
//        PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
//        ArrayList<PreparedStatement> updateQueryList = new ArrayList<>();
//
//        System.out.println("encrypting password (this might take a while)");
//        while (rs.next()) {
//            // get the ID and plain text password from current table
//            String id = rs.getString("id");
//            String password = rs.getString("password");
//
//            // encrypt the password using StrongPasswordEncryptor
//            String encryptedPassword = passwordEncryptor.encryptPassword(password);
//
//            // generate the update query
//            PreparedStatement updateStmt = connection.prepareStatement("UPDATE customers SET password = ? WHERE id = ?");
//            updateStmt.setString(1, encryptedPassword);
//            updateStmt.setString(2, id);
//            updateQueryList.add(updateStmt);
//        }
//        rs.close();
//
//        // execute the update queries to update the password
//        System.out.println("updating password");
//        int count = 0;
//        for (PreparedStatement updateStmt : updateQueryList) {
//            count += updateStmt.executeUpdate();
//            updateStmt.close();
//        }
//        System.out.println("Customer password update completed, " + count + " rows affected");
//
//        // Employee password update
//        String employeeQuery = "SELECT email, password FROM employees";
//        PreparedStatement selectEmployeeStmt = connection.prepareStatement(employeeQuery);
//        ResultSet employeeRs = selectEmployeeStmt.executeQuery();
//
//        ArrayList<PreparedStatement> employeeUpdateQueryList = new ArrayList<>();
//
//        System.out.println("Encrypting employee passwords (this might take a while)");
//        while (employeeRs.next()) {
//            String email = employeeRs.getString("email");
//            String password = employeeRs.getString("password");
//            String encryptedPassword = passwordEncryptor.encryptPassword(password);
//
//            PreparedStatement employeeUpdate = connection.prepareStatement(
//                    "UPDATE employees SET password = ? WHERE email = ?");
//            employeeUpdate.setString(1, encryptedPassword);
//            employeeUpdate.setString(2, email);
//            employeeUpdateQueryList.add(employeeUpdate);
//        }
//        employeeRs.close();
//
//        System.out.println("Updating employee passwords");
//        int employeeCount = 0;
//        for (PreparedStatement employeeUpdateStmt : employeeUpdateQueryList) {
//            employeeCount += employeeUpdateStmt.executeUpdate();
//            employeeUpdateStmt.close();
//        }
//
//        System.out.println("Employee password update completed, " + employeeCount + " rows affected");
//
//        statement.close();
//        connection.close();
//
//        System.out.println("finished");
//
//    }
//
//}
