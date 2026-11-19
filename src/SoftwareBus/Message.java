package SoftwareBus;

public record Message(int topic, int subtopic, int[] body) {

    @Override
    public String toString() {
        return topic + "_" + subtopic + "_" + bodyToString(body);
    }

    public static Message fromString(String message){
        String[] parts = message.split("_");
        return new Message(Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]), bodyFromString(parts[2]));
    }

    private String bodyToString(int[] b){
        String s = "";
        for(int i = 0; i < b.length-1; i++){
            s += b[i] + "-";
        }
        s += b[b.length-1];
        return s;
    }

    private static int[] bodyFromString(String message){
        String[] parts = message.split("-");
        int [] result = new int[parts.length];
        for(int i = 0; i < parts.length; i++){
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}
