package ControlCenter;

import SoftwareBus.Topics;
import SoftwareBus.Message;
import SoftwareBus.SoftwareBus;
import java.util.*;

public class FloorPanels {
    private final SoftwareBus bus;
    private final UUID id;

    private final Set<String> pendingRequests;

    public FloorPanels(SoftwareBus bus) {
        this.bus = bus;
        this.id = bus.register();

        pendingRequests = new HashSet<>();
    }

    //records and publishes a floor request
    public void requestElevator(int floor, Direction direction) {
        if (floor < 1 || floor > Manager.NUM_FLOORS || direction == Direction.STOPPED) {
            return;
        }

        String request = floor + " " + direction;

        if (pendingRequests.add(request)) {
            int directionValue = direction == Direction.UP ? 1 : 2;

            Message message = new Message(
                    Topics.REQUEST,
                    floor,
                    new int[]{directionValue}
            );

            bus.publish(id, message);
        }
    }

    //removes a pending request
    public void cancelRequest(int floor, Direction direction) {
        pendingRequests.remove(floor + " " + direction);
    }

    //checks whether there are pending requests
    public boolean hasPendingRequests() {
        return !pendingRequests.isEmpty();
    }

    //returns all pending requests
    public List<String> getPendingRequests() {
        return new ArrayList<>(pendingRequests);
    }

    //updates floor panel indicator
    public void updateIndicator(int floor, Direction direction, boolean active) {

        String request = floor + " " + direction;

        if (active) {
            requestElevator(floor, direction);
        } else {
            pendingRequests.remove(request);
        }
    }
}
