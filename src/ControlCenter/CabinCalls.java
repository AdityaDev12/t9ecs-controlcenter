package ControlCenter;

import SoftwareBus.Message;
import SoftwareBus.SoftwareBus;
import java.util.*;

public class CabinCalls {
    private final Map<Integer, Set<Integer>> pendingRequests;

    private final SoftwareBus bus;
    private final UUID id;

    public CabinCalls(SoftwareBus bus) {
        this.bus = bus;
        this.id = bus.register();

        pendingRequests = new HashMap<>();
        int numberOfElevators = Manager.NUM_ELEVATORS;
        for (int i = 1; i <= numberOfElevators; i++) {
            pendingRequests.put(i, new HashSet<>());
        }
    }

    //records a cabin floor request
    public void requestFloor(int elevator, int floor) {
        if (!pendingRequests.containsKey(elevator) || floor < 1 || floor > Manager.NUM_FLOORS) {
            return;
        }

        pendingRequests.get(elevator).add(floor);

        //publish cabin call press
        Message message = new Message(
                Topics.FLOOR_BUTTON,
                elevator,
                floor
        );

        bus.publish(id, message);
    }

    //removes a pending cabin request
    public void cancelRequest(int elevator, int floor) {
        if (pendingRequests.containsKey(elevator)) {
            pendingRequests.get(elevator).remove(floor);
        }
    }

    //checks whether an elevator has pending requests
    public boolean hasPendingRequests(int elevator) {
        return pendingRequests.containsKey(elevator) && !pendingRequests.get(elevator).isEmpty();
    }

    //returns all pending destinations for an elevator
    public int[] getPendingRequests(int elevator) {
        if (!pendingRequests.containsKey(elevator)) {
            return new int[0];
        }

        return pendingRequests.get(elevator).stream().mapToInt(Integer::intValue).toArray();
    }

    //clear cabin button
    public void clearButton(int elevator, int button) {
        //publish clear button
        Message message = new Message(
                Topics.CLEAR_BUTTON,
                elevator,
                button
        );

        bus.publish(id, message);
    }
}
