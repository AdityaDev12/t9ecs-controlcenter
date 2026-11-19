package ControlCenter;

public final class Topics {

    private Topics() {}

//  topics

    public static final int FIRE = 20;


    public static final int FLOOR_BUTTON = 21;


    public static final int CLEAR_BUTTON = 22;


    public static final int REQUEST = 23;

    public static final int GO_TO = 24;

    //  bodies
    public static final int FIRE_ON = 1;
    public static final int FIRE_OFF = 0;

    // REQUEST direction
    public static final int UP = 1;
    public static final int DOWN = 2;

    /** subtopic for a message about the whole building rather than one elevator or floor */
    public static final int BUILDING = 0;
}