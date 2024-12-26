package org.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// DatabaseTransactionManager
class DatabaseTransactionManager implements TransactionManager {
    private static final String CONNECTION_STRING = "jdbc:postgresql://localhost/bankdata?user=postgres&password=password";

    static {
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS transactions;"); // Drop table to ensure schema correctness
                stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                        "id SERIAL PRIMARY KEY, " +
                        "amount INTEGER NOT NULL, " +
                        "date DATE NOT NULL)");
                System.out.println("Database initialized: table 'transactions' is ready.");
            }
        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    @Override
    public void saveTransactions(List<Transaction> transactions) {
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            String sql = "INSERT INTO transactions (amount, date) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Transaction transaction : transactions) {
                    pstmt.setInt(1, transaction.getAmount());
                    pstmt.setDate(2, Date.valueOf(transaction.getDate()));
                    pstmt.executeUpdate();
                }
                System.out.println("Transactions saved to the database.");
            }
        } catch (SQLException e) {
            System.out.println("Error saving transactions: " + e.getMessage());
        }
    }

    @Override
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            String sql = "SELECT amount, date FROM transactions";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int amount = rs.getInt("amount");
                    LocalDate date = rs.getDate("date").toLocalDate();
                    transactions.add(new Transaction(amount, date));
                }
                System.out.println("Transactions loaded from the database.");
            }
        } catch (SQLException e) {
            System.out.println("Error loading transactions: " + e.getMessage());
        }
        return transactions;
    }

    public void deleteTransactionsByDate(LocalDate date) {
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            String sql = "DELETE FROM transactions WHERE date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(date));
                int rowsDeleted = pstmt.executeUpdate();
                System.out.println(rowsDeleted + " transaction(s) deleted for date " + date + ".");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting transactions: " + e.getMessage());
        }
    }
}

// TransactionManager Interface
interface TransactionManager {
    void saveTransactions(List<Transaction> transactions);
    List<Transaction> loadTransactions();
}

// Transaction Class
class Transaction {
    private final int amount;
    private final LocalDate date;

    public Transaction(int amount, LocalDate date) {
        this.amount = amount;
        this.date = date;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void showInfo() {
        System.out.println("Amount: " + amount + ", Date: " + date);
    }
}

// BankAccount Class
abstract class BankAccount {
    private int balance;
    protected List<Transaction> transactions = new ArrayList<>();
    private final TransactionManager transactionManager;

    public BankAccount(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        if (amount > 0) {
            balance += amount;
            addTransaction(amount);
        } else {
            System.out.println("Invalid deposit amount.");
        }
    }

    public void withdraw(int amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            addTransaction(-amount);
        } else {
            System.out.println("Insufficient funds or invalid amount.");
        }
    }

    private void addTransaction(int amount) {
        transactions.add(new Transaction(amount, LocalDate.now()));
    }

    public void saveTransactions() {
        transactionManager.saveTransactions(transactions);
    }

    public void loadTransactions() {
        transactions = transactionManager.loadTransactions();
    }

    protected TransactionManager getTransactionManager() {
        return transactionManager;
    }
}

// SimpleBankAccount Class
class SimpleBankAccount extends BankAccount {
    private final Scanner scanner = new Scanner(System.in);

    public SimpleBankAccount(TransactionManager transactionManager) {
        super(transactionManager);
    }

    public void showMenu() {
        char choice;
        do {
            System.out.println("\n1: Check Balance\n2: Deposit\n3: Withdraw\n4: Show All Transactions\n5: Save Transactions\n6: Load Transactions\n7: Delete Transactions by Date\n0: Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextLine().trim().charAt(0);

            switch (choice) {
                case '1' -> System.out.println("Balance: " + getBalance());
                case '2' -> deposit(getIntInput("Enter amount to deposit: "));
                case '3' -> withdraw(getIntInput("Enter amount to withdraw: "));
                case '4' -> transactions.forEach(Transaction::showInfo);
                case '5' -> saveTransactions();
                case '6' -> loadTransactions();
                case '7' -> deleteTransactions();
                case '0' -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != '0');
    }

    private int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a valid integer.");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return value;
    }

    private void deleteTransactions() {
        System.out.print("Enter the date of transactions to delete (YYYY-MM-DD): ");
        String dateInput = scanner.nextLine().trim();
        try {
            LocalDate date = LocalDate.parse(dateInput);
            TransactionManager manager = getTransactionManager();
            if (manager instanceof DatabaseTransactionManager dbManager) {
                dbManager.deleteTransactionsByDate(date);
            } else {
                System.out.println("Transaction deletion is not supported for the current manager.");
            }
        } catch (Exception e) {
            System.out.println("Invalid date format. Please use YYYY-MM-DD.");
        }
    }
}

// Main Class
public class Main {
    public static void main(String[] args) {
        TransactionManager transactionManager = new DatabaseTransactionManager();
        SimpleBankAccount account = new SimpleBankAccount(transactionManager);
        account.showMenu();
    }
}

