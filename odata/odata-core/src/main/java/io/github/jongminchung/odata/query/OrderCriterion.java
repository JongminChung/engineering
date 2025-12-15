package io.github.jongminchung.odata.query;

import java.util.Objects;

public final class OrderCriterion {
    private final String field;
    private final Direction direction;

    public OrderCriterion(String field, Direction direction) {
        if (field == null || field.isBlank()) throw new IllegalArgumentException("field");
        this.field = field;
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public String field() {
        return field;
    }

    public Direction direction() {
        return direction;
    }

    @Override
    public String toString() {
        return field + (direction == Direction.DESC ? " desc" : " asc");
    }
}
