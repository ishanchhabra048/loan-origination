
package com.ishanchhabra.loan_origination.dao;

import com.ishanchhabra.loan_origination.model.Customer;
import com.ishanchhabra.loan_origination.utility.DbConnectionFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

@Repository
public class CustomerDAO {

    private final DbConnectionFactory connectionFactory;

    // Constructor Injection
    public CustomerDAO(DbConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    // 1. Register a new customer
    public long save(Customer customer) throws SQLException {

        String sql = """
                INSERT INTO customer
                (name, date_of_birth, mobile, address, kyc_details)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getName());
            statement.setDate(
                    2, Date.valueOf(customer.getDateOfBirth())
            );
            statement.setString(3, customer.getMobile());
            statement.setString(4, customer.getAddress());
            statement.setString(5, customer.getKycDetails());

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected != 1) {
                throw new SQLException("Failed to save customer");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (keys.next()) {
                    long customerId = keys.getLong(1);
                    customer.setCustomerId(customerId);
                    return customerId;
                }

                throw new SQLException("Customer ID not generated");
            }
        }
    }

    // 2. Find customer by ID
    public Customer findById(long customerId) throws SQLException {

        String sql = """
                SELECT * FROM customer
                WHERE customer_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    // 3. Search customers by name
    public List<Customer> findByName(String name) throws SQLException {

        String sql = """
                SELECT * FROM customer
                WHERE name LIKE ?
                ORDER BY name
                """;

        List<Customer> customers = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, "%" + name + "%");

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
        }

        return customers;
    }

    // 4. Find customer by mobile number
    public Customer findByMobile(String mobile) throws SQLException {

        String sql = """
                SELECT * FROM customer
                WHERE mobile = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, mobile);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    // 5. Convert ResultSet row into Customer object
    private Customer mapRow(ResultSet rs) throws SQLException {

        Customer customer = new Customer();

        customer.setCustomerId(rs.getLong("customer_id"));
        customer.setName(rs.getString("name"));

        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            customer.setDateOfBirth(dob.toLocalDate());
        }

        customer.setMobile(rs.getString("mobile"));
        customer.setAddress(rs.getString("address"));
        customer.setKycDetails(rs.getString("kyc_details"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            customer.setCreatedAt(createdAt.toLocalDateTime());
        }

        return customer;
    }
}
