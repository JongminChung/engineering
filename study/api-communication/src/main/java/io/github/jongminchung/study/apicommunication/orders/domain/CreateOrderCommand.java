package io.github.jongminchung.study.apicommunication.orders.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CreateOrderCommand {

    private final String customerId;
    private final List<String> productCodes;
    private final BigDecimal totalAmount;

    public CreateOrderCommand(String customerId, List<String> productCodes, BigDecimal totalAmount) {
        this.customerId = Objects.requireNonNull(customerId, "customerId must be provided");
        this.productCodes = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(productCodes, "productCodes must be provided")));
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must be provided");
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<String> getProductCodes() {
        return productCodes;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
