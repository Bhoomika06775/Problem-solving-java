import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Task 01: Salary Transformation System
 * Concepts: Functional Programming, Lambda Expressions, Functional Interfaces,
 *           Method References, and Java Stream API.
 */
public class SalaryTransformationSystem {

    // Immutable Employee Model
    public static class Employee {
        private final int id;
        private final String name;
        private final String department;
        private final double baseSalary;
        private final double performanceRating; // Scale 1.0 to 5.0
        private final int experienceYears;

        public Employee(int id, String name, String department, double baseSalary, double performanceRating, int experienceYears) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.baseSalary = baseSalary;
            this.performanceRating = performanceRating;
            this.experienceYears = experienceYears;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getBaseSalary() { return baseSalary; }
        public double getPerformanceRating() { return performanceRating; }
        public int getExperienceYears() { return experienceYears; }

        @Override
        public String toString() {
            return String.format("[%d] %-15s | Dept: %-10s | Base: $%-9.2f | Rating: %.1f | Exp: %d yrs",
                    id, name, department, baseSalary, performanceRating, experienceYears);
        }
    }

    // Processed Salary Record
    public static class SalarySlip {
        private final Employee employee;
        private final double hra;
        private final double da;
        private final double performanceBonus;
        private final double grossSalary;
        private final double taxDeduction;
        private final double netSalary;

        public SalarySlip(Employee employee, double hra, double da, double performanceBonus,
                          double grossSalary, double taxDeduction, double netSalary) {
            this.employee = employee;
            this.hra = hra;
            this.da = da;
            this.performanceBonus = performanceBonus;
            this.grossSalary = grossSalary;
            this.taxDeduction = taxDeduction;
            this.netSalary = netSalary;
        }

        public Employee getEmployee() { return employee; }
        public double getGrossSalary() { return grossSalary; }
        public double getTaxDeduction() { return taxDeduction; }
        public double getNetSalary() { return netSalary; }

        public void printSummary() {
            System.out.printf("%-5d %-16s %-12s $%-9.2f $%-8.2f $%-8.2f $%-8.2f $%-9.2f $%-8.2f $%-9.2f%n",
                    employee.getId(), employee.getName(), employee.getDepartment(),
                    employee.getBaseSalary(), hra, da, performanceBonus,
                    grossSalary, taxDeduction, netSalary);
        }
    }

    // Custom Functional Interface for complex multi-rule transformation
    @FunctionalInterface
    public interface SalaryTransformer<T, R> {
        R transform(T input);
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("                     SALARY TRANSFORMATION SYSTEM (FUNCTIONAL PIPELINE)                  ");
        System.out.println("=========================================================================================");

        // 1. Initial Dataset
        List<Employee> employees = Arrays.asList(
                new Employee(101, "Alice Smith", "Engineering", 75000, 4.8, 5),
                new Employee(102, "Bob Johnson", "Finance",     62000, 3.9, 3),
                new Employee(103, "Charlie Brown", "Engineering", 95000, 4.9, 8),
                new Employee(104, "Diana Prince", "HR",          54000, 4.2, 4),
                new Employee(105, "Ethan Hunt", "Sales",        68000, 4.5, 6),
                new Employee(106, "Fiona Gallagher", "Finance",  82000, 3.6, 7),
                new Employee(107, "George Clark", "Sales",       48000, 2.9, 2),
                new Employee(108, "Hannah Abbott", "Engineering", 88000, 4.6, 6)
        );

        System.out.println("\n--- Initial Employee Records ---");
        employees.forEach(System.out::println);

        // 2. Functional Definitions for Allowances and Bonuses
        Function<Employee, Double> calculateHRA = emp -> emp.getBaseSalary() * 0.20; // 20% House Rent Allowance
        Function<Employee, Double> calculateDA  = emp -> emp.getBaseSalary() * 0.10; // 10% Dearness Allowance

        // Performance Bonus using Function & Predicate rules
        Function<Employee, Double> calculateBonus = emp -> {
            double rating = emp.getPerformanceRating();
            if (rating >= 4.5) return emp.getBaseSalary() * 0.25;      // 25% for top performers
            else if (rating >= 4.0) return emp.getBaseSalary() * 0.15; // 15% for above average
            else if (rating >= 3.0) return emp.getBaseSalary() * 0.08; // 8% standard
            return 0.0; // below 3.0 no bonus
        };

        // Progressive Tax Deduction Function
        Function<Double, Double> calculateTax = gross -> {
            if (gross > 100000) {
                return (gross - 100000) * 0.30 + (25000 * 0.20) + (25000 * 0.10);
            } else if (gross > 75000) {
                return (gross - 75000) * 0.20 + (25000 * 0.10);
            } else if (gross > 50000) {
                return (gross - 50000) * 0.10;
            }
            return 0.0; // Tax-free up to $50,000
        };

        // 3. Complete Transformation Pipeline: Employee -> SalarySlip
        SalaryTransformer<Employee, SalarySlip> pipeline = emp -> {
            double hra = calculateHRA.apply(emp);
            double da = calculateDA.apply(emp);
            double bonus = calculateBonus.apply(emp);
            double gross = emp.getBaseSalary() + hra + da + bonus;
            double tax = calculateTax.apply(gross);
            double net = gross - tax;
            return new SalarySlip(emp, hra, da, bonus, gross, tax, net);
        };

        // Process all employees into SalarySlips using Streams
        List<SalarySlip> salarySlips = employees.stream()
                .map(pipeline::transform)
                .collect(Collectors.toList());

        System.out.println("\n-------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-5s %-16s %-12s %-10s %-9s %-9s %-9s %-10s %-9s %-10s%n",
                "ID", "Name", "Department", "Base", "HRA", "DA", "Bonus", "Gross", "Tax", "Net Pay");
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        salarySlips.forEach(SalarySlip::printSummary);
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        // 4. Higher-Order Functional Analysis and Filterings
        System.out.println("\n>>> High Net Earners (Net Salary > $85,000) <<<");
        Predicate<SalarySlip> highEarnersPredicate = s -> s.getNetSalary() > 85000;
        salarySlips.stream()
                .filter(highEarnersPredicate)
                .forEach(s -> System.out.printf("  * %s (%s): Net Pay = $%.2f%n",
                        s.getEmployee().getName(), s.getEmployee().getDepartment(), s.getNetSalary()));

        // 5. Department-wise Average Net Salary using Collectors.groupingBy
        System.out.println("\n>>> Department-Wise Average Net Salary <<<");
        Map<String, Double> deptAvgSalary = salarySlips.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEmployee().getDepartment(),
                        Collectors.averagingDouble(SalarySlip::getNetSalary)
                ));

        deptAvgSalary.forEach((dept, avg) ->
                System.out.printf("  * %-12s : Average Net Pay = $%,10.2f%n", dept, avg));

        // 6. Overall Payroll Analytics using DoubleSummaryStatistics
        DoubleSummaryStatistics stats = salarySlips.stream()
                .collect(Collectors.summarizingDouble(SalarySlip::getNetSalary));

        System.out.println("\n>>> Overall Corporate Payroll Summary <<<");
        System.out.printf("  * Total Employees Processed : %d%n", stats.getCount());
        System.out.printf("  * Total Net Payroll Outflow  : $%,.2f%n", stats.getSum());
        System.out.printf("  * Average Net Salary         : $%,.2f%n", stats.getAverage());
        System.out.printf("  * Minimum Net Salary         : $%,.2f%n", stats.getMin());
        System.out.printf("  * Maximum Net Salary         : $%,.2f%n", stats.getMax());
        System.out.println("=========================================================================================");
    }
}
