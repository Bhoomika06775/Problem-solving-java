import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task 05: Banking Transaction System using Object-Oriented Programming (OOP)
 * Concepts: Encapsulation, Abstraction, Inheritance, Polymorphism,
 *           Custom Exceptions, and Transaction Ledger Auditing.
 */
public class BankingTransactionSystem {

    // Custom Domain Exceptions
    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) { super(message); }
    }

    public static class InvalidAmountException extends Exception {
        public InvalidAmountException(String message) { super(message); }
    }

    public static class AccountNotFoundException extends Exception {
        public AccountNotFoundException(String message) { super(message); }
    }

    // Transaction Types & Record
    public enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST, OVERDRAFT_FEE }

    public static class TransactionRecord {
        private final String txnId;
        private final TransactionType type;
        private final double amount;
        private final double balanceAfter;
        private final String timestamp;
        private final String remarks;

        public TransactionRecord(TransactionType type, double amount, double balanceAfter, String remarks) {
            this.txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.remarks = remarks;
        }

        @Override
        public String toString() {
            return String.format("[%s] %-12s | %-14s | Amount: $%-9.2f | Balance: $%-9.2f | Note: %s",
                    timestamp, txnId, type, amount, balanceAfter, remarks);
        }
    }

    // Abstract Base Account (Abstraction & Encapsulation)
    public static abstract class Account {
        private final String accountNumber;
        private final String accountHolderName;
        protected double balance;
        protected final List<TransactionRecord> transactionHistory = new ArrayList<>();

        public Account(String accountNumber, String accountHolderName, double initialDeposit) {
            this.accountNumber = accountNumber;
            this.accountHolderName = accountHolderName;
            this.balance = initialDeposit;
            recordTransaction(TransactionType.DEPOSIT, initialDeposit, "Initial account opening deposit");
        }

        public String getAccountNumber() { return accountNumber; }
        public String getAccountHolderName() { return accountHolderName; }
        public double getBalance() { return balance; }
        public List<TransactionRecord> getTransactionHistory() { return Collections.unmodifiableList(transactionHistory); }

        protected void recordTransaction(TransactionType type, double amount, String remarks) {
            transactionHistory.add(new TransactionRecord(type, amount, this.balance, remarks));
        }

        public void deposit(double amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Deposit amount must be strictly greater than 0.");
            }
            this.balance += amount;
            recordTransaction(TransactionType.DEPOSIT, amount, "Funds credited successfully");
            System.out.printf("  [DEPOSIT OK] Account %s credited with $%.2f. New Balance: $%.2f%n",
                    accountNumber, amount, balance);
        }

        // Abstract method enforcing polymorphism across account variants
        public abstract void withdraw(double amount) throws InsufficientFundsException, InvalidAmountException;

        public void printStatement() {
            System.out.println("=========================================================================================");
            System.out.printf(" Account Statement: %s (%s) | Holder: %s%n",
                    accountNumber, getClass().getSimpleName(), accountHolderName);
            System.out.printf(" Current Available Balance: $%,.2f%n", balance);
            System.out.println("-----------------------------------------------------------------------------------------");
            transactionHistory.forEach(t -> System.out.println("  " + t));
            System.out.println("=========================================================================================\n");
        }
    }

    // Derived Savings Account (Inheritance & Polymorphism)
    public static class SavingsAccount extends Account {
        private final double interestRate; // Annual percentage e.g., 4.5%
        private final double minimumBalance;

        public SavingsAccount(String accountNumber, String accountHolderName, double initialDeposit,
                              double interestRate, double minimumBalance) {
            super(accountNumber, accountHolderName, initialDeposit);
            this.interestRate = interestRate;
            this.minimumBalance = minimumBalance;
        }

        @Override
        public void withdraw(double amount) throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be strictly positive.");
            }
            if (balance - amount < minimumBalance) {
                throw new InsufficientFundsException(String.format(
                        "Withdrawal of $%.2f failed. Minimum balance requirement of $%.2f would be violated. Current balance: $%.2f",
                        amount, minimumBalance, balance));
            }
            balance -= amount;
            recordTransaction(TransactionType.WITHDRAWAL, amount, "Savings withdrawal processed");
            System.out.printf("  [WITHDRAW OK] Savings Account %s debited $%.2f. New Balance: $%.2f%n",
                    getAccountNumber(), amount, balance);
        }

        public void applyMonthlyInterest() {
            double monthlyRate = (interestRate / 100.0) / 12.0;
            double interestEarned = balance * monthlyRate;
            balance += interestEarned;
            recordTransaction(TransactionType.INTEREST, interestEarned,
                    String.format("Monthly interest applied at %.2f%% p.a.", interestRate));
            System.out.printf("  [INTEREST OK] Savings Account %s earned $%.2f interest. New Balance: $%.2f%n",
                    getAccountNumber(), interestEarned, balance);
        }
    }

    // Derived Current/Checking Account (Inheritance & Polymorphism)
    public static class CurrentAccount extends Account {
        private final double overdraftLimit;
        private static final double OVERDRAFT_FEE = 25.0;

        public CurrentAccount(String accountNumber, String accountHolderName, double initialDeposit, double overdraftLimit) {
            super(accountNumber, accountHolderName, initialDeposit);
            this.overdraftLimit = overdraftLimit;
        }

        @Override
        public void withdraw(double amount) throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive.");
            }
            if (balance - amount < -overdraftLimit) {
                throw new InsufficientFundsException(String.format(
                        "Transaction denied! Exceeds allowed overdraft limit of $%.2f. Current balance: $%.2f",
                        overdraftLimit, balance));
            }

            balance -= amount;
            String note = "Current account withdrawal";
            if (balance < 0) {
                balance -= OVERDRAFT_FEE;
                recordTransaction(TransactionType.OVERDRAFT_FEE, OVERDRAFT_FEE, "Overdraft protection fee charged");
                note += " (In Overdraft)";
            }
            recordTransaction(TransactionType.WITHDRAWAL, amount, note);
            System.out.printf("  [WITHDRAW OK] Current Account %s debited $%.2f. New Balance: $%.2f%n",
                    getAccountNumber(), amount, balance);
        }
    }

    // Bank Management Core
    public static class Bank {
        private final String bankName;
        private final Map<String, Account> accounts = new HashMap<>();

        public Bank(String bankName) {
            this.bankName = bankName;
        }

        public void registerAccount(Account account) {
            accounts.put(account.getAccountNumber(), account);
            System.out.printf("  Registered [%s] for %s with Initial Balance: $%.2f%n",
                    account.getAccountNumber(), account.getAccountHolderName(), account.getBalance());
        }

        public Account getAccount(String accountNumber) throws AccountNotFoundException {
            Account acc = accounts.get(accountNumber);
            if (acc == null) {
                throw new AccountNotFoundException("Account number not found: " + accountNumber);
            }
            return acc;
        }

        public void transferFunds(String fromAccNum, String toAccNum, double amount)
                throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
            Account fromAcc = getAccount(fromAccNum);
            Account toAcc = getAccount(toAccNum);

            if (amount <= 0) throw new InvalidAmountException("Transfer amount must be positive.");

            System.out.printf("\n>>> Initiating Wire Transfer of $%.2f from %s to %s <<<%n", amount, fromAccNum, toAccNum);
            fromAcc.withdraw(amount);
            toAcc.balance += amount;
            toAcc.recordTransaction(TransactionType.TRANSFER_IN, amount, "Transfer received from " + fromAccNum);
            fromAcc.recordTransaction(TransactionType.TRANSFER_OUT, amount, "Transfer sent to " + toAccNum);
            System.out.printf("  [TRANSFER SUCCESS] $%.2f transferred from %s to %s%n", amount, fromAccNum, toAccNum);
        }

        public void displayBankSummary() {
            System.out.println("\n=========================================================================================");
            System.out.println("                         " + bankName.toUpperCase() + " OVERVIEW                        ");
            System.out.println("=========================================================================================");
            double totalDeposits = 0;
            for (Account acc : accounts.values()) {
                System.out.printf("  * %-10s | %-16s | %-15s | Balance: $%,10.2f%n",
                        acc.getAccountNumber(), acc.getAccountHolderName(), acc.getClass().getSimpleName(), acc.getBalance());
                totalDeposits += acc.getBalance();
            }
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("  Total Customer Accounts : %d%n", accounts.size());
            System.out.printf("  Total Liquidity Reserve : $%,.2f%n", totalDeposits);
            System.out.println("=========================================================================================\n");
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            BANKING TRANSACTION SYSTEM (OBJECT-ORIENTED ARCHITECTURE)                    ");
        System.out.println("=========================================================================================");

        Bank apexBank = new Bank("Apex Global Banking Corp");

        // 1. Account Creation
        System.out.println("\n--- Step 1: Account Creation & Onboarding ---");
        SavingsAccount sa1 = new SavingsAccount("SA-101", "Emma Watson", 5000.0, 4.0, 500.0);
        CurrentAccount ca1 = new CurrentAccount("CA-201", "Bruce Wayne", 12000.0, 5000.0);
        SavingsAccount sa2 = new SavingsAccount("SA-102", "Peter Parker", 1500.0, 3.5, 300.0);

        apexBank.registerAccount(sa1);
        apexBank.registerAccount(ca1);
        apexBank.registerAccount(sa2);

        // 2. Perform Transactions
        System.out.println("\n--- Step 2: Executing Regular Transactions ---");
        try {
            sa1.deposit(1200.0);
            sa1.withdraw(800.0);
            sa1.applyMonthlyInterest();

            ca1.withdraw(15000.0); // Uses overdraft!
            ca1.deposit(2000.0);
        } catch (Exception e) {
            System.err.println("Transaction Error: " + e.getMessage());
        }

        // 3. Inter-Account Fund Transfer
        try {
            apexBank.transferFunds("SA-101", "SA-102", 1500.0);
        } catch (Exception e) {
            System.err.println("Transfer Failed: " + e.getMessage());
        }

        // 4. Exception Handling Validation (Overdraw & Violation Checks)
        System.out.println("\n--- Step 3: Exception Handling & Rule Enforcement ---");
        try {
            System.out.println("Attempting to withdraw $4,000 from Peter's account (Balance: $3,000, Min: $300)...");
            sa2.withdraw(4000.0);
        } catch (InsufficientFundsException | InvalidAmountException e) {
            System.out.println("  [CAUGHT EXPECTED EXCEPTION]: " + e.getMessage());
        }

        try {
            System.out.println("\nAttempting negative deposit (-$500)...");
            sa1.deposit(-500.0);
        } catch (InvalidAmountException e) {
            System.out.println("  [CAUGHT EXPECTED EXCEPTION]: " + e.getMessage());
        }

        // 5. Account Statements & Bank Ledger Summary
        System.out.println("\n--- Step 4: Printing Detailed Account Statements ---");
        sa1.printStatement();
        ca1.printStatement();

        // 6. Overall Bank Liquidity
        apexBank.displayBankSummary();
    }
}
