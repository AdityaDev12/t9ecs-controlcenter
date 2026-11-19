package ControlCenter;

import SoftwareBus.Message;
import SoftwareBus.SoftwareBus;
import java.util.UUID;

public class FireAlarm {
    private boolean active;

    private final SoftwareBus bus;
    private final UUID id;

    public FireAlarm(SoftwareBus bus) {
        this.bus = bus;
        this.id = bus.register();
        active = false;
    }

    public void activateFire() {
        active = true;
        publishFireStatus();
    }

    public void resetFire() {
        active = false;
        publishFireStatus();
    }

    public boolean isFireActive() {
        return active;
    }

    private void publishFireStatus() {
        Message message = new Message(
                Topics.FIRE,
                0,
                active ? 1 : 0
        );

        bus.publish(id, message);
    }
}