package ControlCenter;

import SoftwareBus.SoftwareBus;
import SoftwareBus.Topics;
import SoftwareBus.Message;

import java.util.UUID;

// Entry point of the Control Center.
// Creates the Software Bus and the components that use it, then starts the Manager.
// Main contains no control logic; every decision is made by the Manager.
public class Main {

    public static void main(String[] args) {
        SoftwareBus bus;
        if (args.length == 2) {
            bus = new SoftwareBus(args[0], Integer.parseInt(args[1]));
        } else if (args.length == 0) {
            bus = new SoftwareBus();
        } else {
            System.err.println("Usage: Main [router-ip router-port]");
            return;
        }

        FireAlarm fireAlarm = new FireAlarm(bus);
        CabinCalls cabinCalls = new CabinCalls(bus);
        FloorPanels floorPanels = new FloorPanels(bus);
        Elevators elevators = new Elevators(bus);

        Manager manager = new Manager(fireAlarm, cabinCalls, floorPanels, elevators);

        // TEMPORARY TEST BEGINS HERE
        // multiple destinations for elevator 1
        cabinCalls.requestFloor(1,5);
        cabinCalls.requestFloor(1,8);
        // temporary simulated Elevator Control Board
        UUID testId = bus.register();
        Thread simulator = new Thread(() -> {
            try {
                // simulate arriving at floor 5
                Thread.sleep(2000);
                System.out.println("\n[TEST] simulating arrival at floor 5");
                bus.publish(testId,
                        new Message(Topics.ARRIVED, 1, new int[]{5}));
                // simulate arriving at floor 8
                Thread.sleep(2000);
                System.out.println("\n[TEST] simulating arrival at floor 8");
                bus.publish(testId,
                        new Message(Topics.ARRIVED, 1, new int[]{8}));
                // request lower floor after reaching floor 8
                Thread.sleep(1000);
                System.out.println("\n[TEST] requesting floor 3");
                cabinCalls.requestFloor(1, 3);
                // simulate arriving at floor 3
                Thread.sleep(2000);
                System.out.println("\n[TEST] simulating arrival at floor 3");
                bus.publish(testId,
                        new Message(Topics.ARRIVED, 1, new int[]{3}));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        simulator.start();
        // TEMPORARY TEST ENDS HERE

        System.out.println("Control Center started.");
        manager.run();

        System.out.println("Control Center started.");
        manager.run();
    }
}
