package xd.harm.utils.animations;

public enum Direction2 {
    FORWARDS,
    BACKWARDS;

    public Direction2 opposite() {
        if (this == Direction2.FORWARDS) {
            return Direction2.BACKWARDS;
        } else return Direction2.FORWARDS;
    }

}
