import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task 06: Ride Sharing Platform Simulator Using Object-Oriented Programming (OOP)
 * Concepts: Encapsulation, Abstraction, Polymorphism, Strategy Pattern for Pricing,
 *           State Management, and Dispatch Coordination.
 */
public class RideSharingPlatformSimulator {

    // 2D Spatial Location with Euclidean distance calculation
    public static class Location {
        private final double x;
        private final double y;
        private final String name;

        public Location(double x, double y, String name) {
            this.x = x;
            this.y = y;
            this.name = name;
        }

        public double distanceTo(Location other) {
            return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
        }

        public String getName() { return name; }

        @Override
        public String toString() {
            return String.format("%s (%.1f, %.1f)", name, x, y);
        }
    }

    // Vehicle Category
    public enum VehicleType {
        ECONOMY(5.0, 1.2),   // Base fare, per km rate
        SEDAN(8.0, 1.8),
        PREMIUM_SUV(15.0, 2.5);

        private final double baseFare;
        private final double perKmRate;

        VehicleType(double baseFare, double perKmRate) {
            this.baseFare = baseFare;
            this.perKmRate = perKmRate;
        }

        public double getBaseFare() { return baseFare; }
        public double getPerKmRate() { return perKmRate; }
    }

    // Fare Strategy Pattern (Polymorphism)
    public interface FareStrategy {
        double calculateFare(VehicleType vehicleType, double distanceKm);
    }

    public static class StandardFareStrategy implements FareStrategy {
        @Override
        public double calculateFare(VehicleType vehicleType, double distanceKm) {
            return vehicleType.getBaseFare() + (vehicleType.getPerKmRate() * distanceKm);
        }
    }

    public static class SurgeFareStrategy implements FareStrategy {
        private final double surgeMultiplier;

        public SurgeFareStrategy(double surgeMultiplier) {
            this.surgeMultiplier = surgeMultiplier;
        }

        @Override
        public double calculateFare(VehicleType vehicleType, double distanceKm) {
            double standard = vehicleType.getBaseFare() + (vehicleType.getPerKmRate() * distanceKm);
            return standard * surgeMultiplier;
        }
    }

    // Abstract User
    public static abstract class User {
        private final String id;
        private final String name;
        private final String phone;

        public User(String id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
    }

    // Rider Entity
    public static class Rider extends User {
        private double walletBalance;

        public Rider(String id, String name, String phone, double initialWallet) {
            super(id, name, phone);
            this.walletBalance = initialWallet;
        }

        public double getWalletBalance() { return walletBalance; }

        public void debitWallet(double amount) {
            this.walletBalance -= amount;
        }

        public void addFunds(double amount) {
            this.walletBalance += amount;
        }
    }

    // Driver Entity
    public static class Driver extends User {
        private Location currentLocation;
        private final VehicleType vehicleType;
        private final String vehiclePlate;
        private boolean isAvailable;
        private double earnings;
        private double ratingSum = 0;
        private int ratingCount = 0;

        public Driver(String id, String name, String phone, Location initialLocation,
                      VehicleType vehicleType, String vehiclePlate) {
            super(id, name, phone);
            this.currentLocation = initialLocation;
            this.vehicleType = vehicleType;
            this.vehiclePlate = vehiclePlate;
            this.isAvailable = true;
            this.earnings = 0.0;
        }

        public Location getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(Location loc) { this.currentLocation = loc; }
        public VehicleType getVehicleType() { return vehicleType; }
        public String getVehiclePlate() { return vehiclePlate; }
        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }
        public double getEarnings() { return earnings; }
        public void addEarnings(double amount) { this.earnings += amount; }

        public void addRating(int stars) {
            ratingSum += stars;
            ratingCount++;
        }

        public double getAverageRating() {
            return ratingCount == 0 ? 5.0 : ratingSum / ratingCount;
        }

        @Override
        public String toString() {
            return String.format("[%s] Driver %-14s | Vehicle: %-11s (%s) | Rating: %.1f★ | Loc: %s",
                    getId(), getName(), vehicleType, vehiclePlate, getAverageRating(), currentLocation.getName());
        }
    }

    // Ride Lifecycle & State Management
    public enum RideStatus { REQUESTED, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED }

    public static class Ride {
        private final String rideId;
        private final Rider rider;
        private Driver driver;
        private final Location pickup;
        private final Location dropoff;
        private final VehicleType vehicleType;
        private double fare;
        private RideStatus status;
        private final String requestTime;

        public Ride(String rideId, Rider rider, Location pickup, Location dropoff, VehicleType vehicleType) {
            this.rideId = rideId;
            this.rider = rider;
            this.pickup = pickup;
            this.dropoff = dropoff;
            this.vehicleType = vehicleType;
            this.status = RideStatus.REQUESTED;
            this.requestTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        public String getRideId() { return rideId; }
        public Rider getRider() { return rider; }
        public Driver getDriver() { return driver; }
        public void setDriver(Driver driver) { this.driver = driver; }
        public Location getPickup() { return pickup; }
        public Location getDropoff() { return dropoff; }
        public VehicleType getVehicleType() { return vehicleType; }
        public double getFare() { return fare; }
        public void setFare(double fare) { this.fare = fare; }
        public RideStatus getStatus() { return status; }
        public void setStatus(RideStatus status) { this.status = status; }

        public double getTripDistance() {
            return pickup.distanceTo(dropoff);
        }
    }

    // Platform Coordinator Engine
    public static class RideSharingPlatform {
        private final List<Driver> drivers = new ArrayList<>();
        private final List<Ride> completedRides = new ArrayList<>();
        private static final double PLATFORM_COMMISSION_RATE = 0.20; // 20% platform fee

        public void registerDriver(Driver driver) {
            drivers.add(driver);
        }

        public Ride requestRide(Rider rider, Location pickup, Location dropoff,
                                VehicleType preferredType, FareStrategy strategy) {
            System.out.println("\n-----------------------------------------------------------------------------------------");
            System.out.printf(">>> Rider %s requested a %s ride from '%s' to '%s'%n",
                    rider.getName(), preferredType, pickup.getName(), dropoff.getName());

            // 1. Find nearest available driver matching vehicle type
            Driver matchedDriver = drivers.stream()
                    .filter(Driver::isAvailable)
                    .filter(d -> d.getVehicleType() == preferredType)
                    .min(Comparator.comparingDouble(d -> d.getCurrentLocation().distanceTo(pickup)))
                    .orElse(null);

            if (matchedDriver == null) {
                System.out.println("  [ERROR] No available drivers matching criteria in proximity!");
                return null;
            }

            String rideId = "RIDE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            Ride ride = new Ride(rideId, rider, pickup, dropoff, preferredType);
            ride.setDriver(matchedDriver);
            matchedDriver.setAvailable(false);
            ride.setStatus(RideStatus.MATCHED);

            double distance = ride.getTripDistance();
            double fare = strategy.calculateFare(preferredType, distance);
            ride.setFare(fare);

            double pickupDist = matchedDriver.getCurrentLocation().distanceTo(pickup);
            System.out.printf("  [MATCH SUCCESS] Assigned Driver: %s (Car: %s, Rating: %.1f★)%n",
                    matchedDriver.getName(), matchedDriver.getVehiclePlate(), matchedDriver.getAverageRating());
            System.out.printf("  [DISPATCH] Driver distance to pickup: %.2f km | Trip Distance: %.2f km%n",
                    pickupDist, distance);
            System.out.printf("  [ESTIMATED FARE] $%.2f (Pricing Strategy: %s)%n",
                    fare, strategy.getClass().getSimpleName());

            return ride;
        }

        public void completeRide(Ride ride, int riderRating) {
            if (ride == null || ride.getStatus() == RideStatus.COMPLETED) return;

            Driver driver = ride.getDriver();
            Rider rider = ride.getRider();
            double totalFare = ride.getFare();

            // Validate wallet balance
            if (rider.getWalletBalance() < totalFare) {
                System.out.printf("  [PAYMENT NOTICE] Rider %s had insufficient wallet ($%.2f), auto-reloading $50.00...%n",
                        rider.getName(), rider.getWalletBalance());
                rider.addFunds(50.0);
            }

            // Deduct fare from rider
            rider.debitWallet(totalFare);

            // Split revenue: Platform commission & Driver payout
            double platformCut = totalFare * PLATFORM_COMMISSION_RATE;
            double driverPayout = totalFare - platformCut;
            driver.addEarnings(driverPayout);

            // Update driver state
            driver.setCurrentLocation(ride.getDropoff());
            driver.setAvailable(true);
            driver.addRating(riderRating);

            ride.setStatus(RideStatus.COMPLETED);
            completedRides.add(ride);

            System.out.printf("\n  [TRIP COMPLETED] Ride ID: %s%n", ride.getRideId());
            System.out.printf("  * Trip Distance        : %.2f km%n", ride.getTripDistance());
            System.out.printf("  * Total Fare Charged   : $%.2f%n", totalFare);
            System.out.printf("  * Platform Commission  : $%.2f (20%%)%n", platformCut);
            System.out.printf("  * Driver Net Payout    : $%.2f%n", driverPayout);
            System.out.printf("  * Rider Remaining Bal  : $%.2f%n", rider.getWalletBalance());
            System.out.printf("  * Feedback Submitted   : %d / 5 Stars -> Driver new rating: %.2f★%n",
                    riderRating, driver.getAverageRating());
        }

        public void printPlatformDashboard() {
            System.out.println("\n=========================================================================================");
            System.out.println("                         RIDE SHARING PLATFORM DASHBOARD                         ");
            System.out.println("=========================================================================================");
            System.out.println("Registered Fleet Status:");
            drivers.forEach(d -> System.out.printf("  * %s | Cumulative Payout: $%.2f%n", d, d.getEarnings()));

            double totalPlatformRevenue = completedRides.stream()
                    .mapToDouble(r -> r.getFare() * PLATFORM_COMMISSION_RATE)
                    .sum();

            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("  Total Completed Trips : %d%n", completedRides.size());
            System.out.printf("  Total Platform Cut    : $%,.2f%n", totalPlatformRevenue);
            System.out.println("=========================================================================================\n");
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("               RIDE SHARING PLATFORM SIMULATOR (OOP ARCHITECTURE)                        ");
        System.out.println("=========================================================================================");

        RideSharingPlatform platform = new RideSharingPlatform();

        // City Map Waypoints
        Location downtown = new Location(2.0, 3.0, "Downtown Financial Center");
        Location airport  = new Location(15.0, 22.0, "International Airport");
        Location suburb   = new Location(8.0, 1.0, "Greenwood Suburbs");
        Location techPark = new Location(12.0, 10.0, "Silicon Tech Park");

        // Drivers Fleet
        Driver d1 = new Driver("DRV-01", "Marcus Vance", "555-1001", downtown, VehicleType.SEDAN, "ABC-1234");
        Driver d2 = new Driver("DRV-02", "Sarah Connor", "555-1002", suburb,   VehicleType.ECONOMY, "XYZ-9876");
        Driver d3 = new Driver("DRV-03", "James Bond",   "555-1003", techPark, VehicleType.PREMIUM_SUV, "JB-007");
        Driver d4 = new Driver("DRV-04", "Leo Morales",  "555-1004", downtown, VehicleType.ECONOMY, "NY-4521");

        platform.registerDriver(d1);
        platform.registerDriver(d2);
        platform.registerDriver(d3);
        platform.registerDriver(d4);

        // Riders
        Rider riderAlice = new Rider("RDR-01", "Alice Jenkins", "555-2001", 60.0);
        Rider riderBob   = new Rider("RDR-02", "Bob Martinez",  "555-2002", 25.0);

        // Simulation Trip 1: Standard Fare Economy Ride
        FareStrategy standardPricing = new StandardFareStrategy();
        Ride trip1 = platform.requestRide(riderAlice, downtown, techPark, VehicleType.ECONOMY, standardPricing);
        if (trip1 != null) {
            platform.completeRide(trip1, 5);
        }

        // Simulation Trip 2: Surge Pricing Luxury Trip during Rush Hour
        FareStrategy surgePricing = new SurgeFareStrategy(1.5); // 1.5x surge
        Ride trip2 = platform.requestRide(riderBob, techPark, airport, VehicleType.PREMIUM_SUV, surgePricing);
        if (trip2 != null) {
            platform.completeRide(trip2, 4);
        }

        // Platform Dashboard
        platform.printPlatformDashboard();
    }
}
