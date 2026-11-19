package ControlCenter;

import SoftwareBus.SoftwareBus;

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
        System.out.println("Control Center started.");
        manager.run();
    }
}
