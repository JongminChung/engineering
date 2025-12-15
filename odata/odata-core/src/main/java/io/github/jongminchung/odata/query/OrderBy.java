package io.github.jongminchung.odata.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public final class OrderBy<T> {
    private final List<OrderCriterion> criteria;

    private OrderBy(List<OrderCriterion> criteria) {
        this.criteria = List.copyOf(criteria);
    }

    public static <T> OrderBy<T> of(List<OrderCriterion> criteria) {
        return new OrderBy<>(criteria);
    }

    @SafeVarargs
    public static <T> OrderBy<T> of(OrderCriterion... items) {
        List<OrderCriterion> list = new ArrayList<>();
        Collections.addAll(list, items);
        return new OrderBy<>(list);
    }

    public static OrderCriterion asc(String field) {
        return new OrderCriterion(field, Direction.ASC);
    }

    public static OrderCriterion desc(String field) {
        return new OrderCriterion(field, Direction.DESC);
    }

    public static <T> OrderBy<T> parse(String input) {
        if (input == null || input.isBlank()) return null;
        List<OrderCriterion> list = new ArrayList<>();
        int pos = 0;
        for (String part : input.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) continue;
            String[] bits = item.split("\\s+");
            String field = bits[0].trim();
            Direction dir = Direction.ASC;
            if (bits.length > 1) {
                String d = bits[1].toLowerCase(Locale.ROOT);
                if (d.equals("desc")) dir = Direction.DESC;
                else if (!d.equals("asc"))
                    throw new ODataParseException("Invalid order direction '" + bits[1] + "'", pos);
            }
            list.add(new OrderCriterion(field, dir));
            pos += part.length() + 1;
        }
        return new OrderBy<>(list);
    }

    public List<OrderCriterion> criteria() {
        return criteria;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",");
        for (OrderCriterion c : criteria) sj.add(c.toString());
        return sj.toString();
    }
}
