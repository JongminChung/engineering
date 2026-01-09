package io.github.jongminchung.study.apicommunication.orders.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.github.jongminchung.study.apicommunication.orders.domain.CreateOrderCommand;

public class CreateOrderRequest {

    @NotBlank private String customerId;

    @NotEmpty private List<@NotBlank String> productCodes = new ArrayList<>();

    @NotNull @DecimalMin(value = "0.1", inclusive = true) private BigDecimal totalAmount;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<String> getProductCodes() {
        return productCodes;
    }

    public void setProductCodes(List<String> productCodes) {
        this.productCodes = productCodes == null ? new ArrayList<>() : productCodes;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(customerId, productCodes, totalAmount);
    }
}
