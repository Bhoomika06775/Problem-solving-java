import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task 11: Smart Parking Management System (Use Case 1)
 * Concepts: Real-World System Design, Object-Oriented Domain Modeling,
 *           Multi-Floor Space Allocation, Dynamic Fee Calculation, and Live Dashboard.
 */
public class SmartParkingManagementSystem {

    public enum VehicleType {
        MOTORCYCLE(1.0),
        COMPACT(1.5),
        SUV(2.5),
        EV(2.0); // Electric Vehicle

        private final double rateMultiplier;

        VehicleType(double rateMultiplier) {
            this.rateMultiplier = rateMultiplier;
        }

        public double getRateMultiplier() { return rateMultiplier; }
    }

    public enum SpotType {
        TWO_WHEELER, COMPACT, LARGE, EV_CHARGING
    }

    // Vehicle Entity
    public static class Vehicle {
        private final String licensePlate;
        private final VehicleType type;
        private final String ownerName;

        public Vehicle(String licensePlate, VehicleType type, String ownerName) {
            this.licensePlate = licensePlate;
            this.type = type;
            this.ownerName = ownerName;
        }

        public String getLicensePlate() { return licensePlate; }
        public VehicleType getType() { return type; }
        public String getOwnerName() { return ownerName; }

        @Override
        public String toString() {
            return String.format("%s [%s] (Owner: %s)", licensePlate, type, ownerName);
        }
    }

    // Parking Spot Entity
    public static class ParkingSpot {
        private final String spotId;
        private final int floor;
        private final SpotType spotType;
        private boolean occupied;
        private Vehicle currentVehicle;

        public ParkingSpot(String spotId, int floor, SpotType spotType) {
            this.spotId = spotId;
            this.floor = floor;
            this.spotType = spotType;
            this.occupied = false;
        }

        public String getSpotId() { return spotId; }
        public int getFloor() { return floor; }
        public SpotType getSpotType() { return spotType; }
        public boolean isOccupied() { return occupied; }
        public Vehicle getCurrentVehicle() { return currentVehicle; }

        public boolean canFitVehicle(Vehicle v) {
            if (occupied) return false;
            switch (v.getType()) {
                case MOTORCYCLE:
                    return spotType == SpotType.TWO_WHEELER || spotType == SpotType.COMPACT;
                case COMPACT:
                    return spotType == SpotType.COMPACT || spotType == SpotType.LARGE;
                case SUV:
                    return spotType == SpotType.LARGE;
                case EV:
                    return spotType == SpotType.EV_CHARGING || spotType == SpotType.COMPACT;
                default:
                    return false;
            }
        }

        public void park(Vehicle vehicle) {
            this.currentVehicle = vehicle;
            this.occupied = true;
        }

        public void vacate() {
            this.currentVehicle = null;
            this.occupied = false;
        }
    }

    // Parking Ticket
    public static class ParkingTicket {
        private final String ticketId;
        private final Vehicle vehicle;
        private final ParkingSpot spot;
        private final long entryEpochMinutes;
        private long exitEpochMinutes;
        private double totalFee;
        private boolean isPaid;

        public ParkingTicket(String ticketId, Vehicle vehicle, ParkingSpot spot, long entryEpochMinutes) {
            this.ticketId = ticketId;
            this.vehicle = vehicle;
            this.spot = spot;
            this.entryEpochMinutes = entryEpochMinutes;
            this.isPaid = false;
        }

        public String getTicketId() { return ticketId; }
        public Vehicle getVehicle() { return vehicle; }
        public ParkingSpot getSpot() { return spot; }
        public long getEntryEpochMinutes() { return entryEpochMinutes; }
        public long getExitEpochMinutes() { return exitEpochMinutes; }
        public double getTotalFee() { return totalFee; }
        public boolean isPaid() { return isPaid; }

        public void completeCheckout(long exitEpochMinutes, double totalFee) {
            this.exitEpochMinutes = exitEpochMinutes;
            this.totalFee = totalFee;
            this.isPaid = true;
        }
    }

    // Parking Lot Management Controller
    public static class SmartParkingLot {
        private final String lotName;
        private final int totalFloors;
        private final List<ParkingSpot> spots = new ArrayList<>();
        private final Map<String, ParkingTicket> activeTickets = new HashMap<>();
        private final List<ParkingTicket> historyTickets = new ArrayList<>();
        private double totalRevenue = 0.0;

        // Base Tariffs
        private static final double BASE_FIRST_HOUR = 3.00;
        private static final double HOURLY_RATE = 2.00;
        private static final double EV_CHARGING_SURCHARGE = 5.00;

        public SmartParkingLot(String lotName, int totalFloors) {
            this.lotName = lotName;
            this.totalFloors = totalFloors;
            initializeSlots();
        }

        private void initializeSlots() {
            for (int f = 1; f <= totalFloors; f++) {
                // Per floor: 2 Bike spots, 4 Compact spots, 3 SUV spots, 2 EV spots
                for (int i = 1; i <= 2; i++) spots.add(new ParkingSpot(String.format("F%d-B%02d", f, i), f, SpotType.TWO_WHEELER));
                for (int i = 1; i <= 4; i++) spots.add(new ParkingSpot(String.format("F%d-C%02d", f, i), f, SpotType.COMPACT));
                for (int i = 1; i <= 3; i++) spots.add(new ParkingSpot(String.format("F%d-L%02d", f, i), f, SpotType.LARGE));
                for (int i = 1; i <= 2; i++) spots.add(new ParkingSpot(String.format("F%d-EV%02d", f, i), f, SpotType.EV_CHARGING));
            }
        }

        public ParkingTicket parkVehicle(Vehicle vehicle, long currentSimMinutes) {
            System.out.printf("\n>>> Incoming Vehicle: %s requested entry <<<%n", vehicle);

            // Optimal spot allocation: lowest floor first, ideal spot match
            ParkingSpot bestSpot = spots.stream()
                    .filter(s -> s.canFitVehicle(vehicle))
                    .min(Comparator.comparingInt(ParkingSpot::getFloor)
                            .thenComparing(s -> s.getSpotType().ordinal()))
                    .orElse(null);

            if (bestSpot == null) {
                System.out.println("  [ENTRY DENIED] Parking Lot is FULL for vehicle category: " + vehicle.getType());
                return null;
            }

            bestSpot.park(vehicle);
            String ticketId = "TCK-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, bestSpot, currentSimMinutes);
            activeTickets.put(ticketId, ticket);

            System.out.printf("  [TICKET ISSUED] Ticket ID: %s%n", ticketId);
            System.out.printf("  [SLOT ASSIGNED] Floor %d | Slot: %s (%s)%n",
                    bestSpot.getFloor(), bestSpot.getSpotId(), bestSpot.getSpotType());
            return ticket;
        }

        public double checkoutVehicle(String ticketId, long exitSimMinutes) {
            ParkingTicket ticket = activeTickets.get(ticketId);
            if (ticket == null) {
                System.out.println("  [ERROR] Invalid or expired Ticket ID: " + ticketId);
                return 0.0;
            }

            long durationMinutes = Math.max(1, exitSimMinutes - ticket.getEntryEpochMinutes());
            double hoursBilled = Math.ceil(durationMinutes / 60.0);

            // Dynamic Fee calculation: Base rate + hourly * multiplier + EV surcharge
            double vehicleMultiplier = ticket.getVehicle().getType().getRateMultiplier();
            double fee = BASE_FIRST_HOUR * vehicleMultiplier;
            if (hoursBilled > 1) {
                fee += (hoursBilled - 1) * HOURLY_RATE * vehicleMultiplier;
            }
            if (ticket.getSpot().getSpotType() == SpotType.EV_CHARGING) {
                fee += EV_CHARGING_SURCHARGE;
            }

            ticket.completeCheckout(exitSimMinutes, fee);
            ticket.getSpot().vacate();
            activeTickets.remove(ticketId);
            historyTickets.add(ticket);
            totalRevenue += fee;

            System.out.println("\n-----------------------------------------------------------------------------------------");
            System.out.printf("  [EXIT BILLING] Vehicle: %s (Plate: %s)%n",
                    ticket.getVehicle().getOwnerName(), ticket.getVehicle().getLicensePlate());
            System.out.printf("  * Spot Freed         : %s (Floor %d)%n", ticket.getSpot().getSpotId(), ticket.getSpot().getFloor());
            System.out.printf("  * Park Duration      : %d minutes (~%.0f billable hours)%n", durationMinutes, hoursBilled);
            System.out.printf("  * Vehicle Type Rate  : %s (%.1fx multiplier)%n", ticket.getVehicle().getType(), vehicleMultiplier);
            if (ticket.getSpot().getSpotType() == SpotType.EV_CHARGING) {
                System.out.println("  * EV Fast Charging   : +$5.00 flat fee applied");
            }
            System.out.printf("  * Total Fee Due/Paid : $%.2f%n", fee);
            System.out.println("-----------------------------------------------------------------------------------------");
            return fee;
        }

        public void printLiveDashboard() {
            System.out.println("\n=========================================================================================");
            System.out.printf("                  LIVE DASHBOARD: %s                  %n", lotName.toUpperCase());
            System.out.println("=========================================================================================");

            long totalSlots = spots.size();
            long occupiedSlots = spots.stream().filter(ParkingSpot::isOccupied).count();
            long availableSlots = totalSlots - occupiedSlots;
            double occupancyPct = ((double) occupiedSlots / totalSlots) * 100.0;

            System.out.printf("Total Capacity: %d Slots | Occupied: %d | Available: %d | Occupancy: %.1f%%%n",
                    totalSlots, occupiedSlots, availableSlots, occupancyPct);
            System.out.printf("Cumulative Revenue Collected: $%,.2f%n", totalRevenue);
            System.out.println("-----------------------------------------------------------------------------------------");

            System.out.println("Floor-by-Floor Occupancy Status:");
            for (int f = 1; f <= totalFloors; f++) {
                final int currentFloor = f;
                List<ParkingSpot> floorSpots = spots.stream()
                        .filter(s -> s.getFloor() == currentFloor)
                        .toList();

                long occ = floorSpots.stream().filter(ParkingSpot::isOccupied).count();
                System.out.printf("  [Floor %d] Occupied: %d / %d slots | Slots: ", f, occ, floorSpots.size());
                for (ParkingSpot s : floorSpots) {
                    System.out.printf("[%s:%s] ", s.getSpotId(), s.isOccupied() ? "OCCUPIED" : "OPEN");
                }
                System.out.println();
            }
            System.out.println("=========================================================================================\n");
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            SMART PARKING MANAGEMENT SYSTEM (USE CASE 1)                                 ");
        System.out.println("=========================================================================================");

        // Create a 2-Floor Smart Garage
        SmartParkingLot garage = new SmartParkingLot("CyberHub Metro Parking Plaza", 2);

        garage.printLiveDashboard();

        // Simulate Vehicles Arriving
        Vehicle v1 = new Vehicle("KA-01-AB-1234", VehicleType.COMPACT, "David Miller");
        Vehicle v2 = new Vehicle("KA-05-EV-9900", VehicleType.EV, "Sophia Turner");
        Vehicle v3 = new Vehicle("MH-12-TX-5678", VehicleType.SUV, "Vikram Malhotra");
        Vehicle v4 = new Vehicle("DL-03-BK-4321", VehicleType.MOTORCYCLE, "Rohan Sharma");

        long timeSim = 100; // Simulated time in minutes

        ParkingTicket t1 = garage.parkVehicle(v1, timeSim);
        ParkingTicket t2 = garage.parkVehicle(v2, timeSim + 15);
        ParkingTicket t3 = garage.parkVehicle(v3, timeSim + 30);
        ParkingTicket t4 = garage.parkVehicle(v4, timeSim + 45);

        // Display updated occupancy
        garage.printLiveDashboard();

        // Simulate Checkouts
        garage.checkoutVehicle(t1.getTicketId(), timeSim + 140); // 40 minutes parked
        garage.checkoutVehicle(t2.getTicketId(), timeSim + 195); // 180 minutes parked with EV charging

        // Final Dashboard view
        garage.printLiveDashboard();
    }
}
