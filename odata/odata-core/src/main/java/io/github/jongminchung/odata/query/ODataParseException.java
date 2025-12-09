package io.github.jongminchung.odata.query;

public class ODataParseException extends RuntimeException {
    private final int position;

    public ODataParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
