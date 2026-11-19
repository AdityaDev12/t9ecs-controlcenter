package ControlCenter;

import SoftwareBus.Topics;
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
        bus.subscribe(id, Topics.FIRE, 0);
    }

    public void activateFire() {
        active = true;
        publishFireStatus();
    }

    public void resetFire() {
        active = false;
        publishFireStatus();
    }

    // receives fire alarm status from other components
    public void pollFireStatus() {
        Message message;
        while ((message = bus.getMsg(id, Topics.FIRE, 0)) != null) {
            int[] body = message.body();
            if (body.length > 0) {
                active = body[0] != 0;
            }
        }
    }

    public boolean isFireActive() {
        return active;
    }

    private void publishFireStatus() {
        Message message = new Message(
                Topics.FIRE,
                0,
                new int[]{active ? 1 : 0}
        );

        bus.publish(id, message);
    }
}