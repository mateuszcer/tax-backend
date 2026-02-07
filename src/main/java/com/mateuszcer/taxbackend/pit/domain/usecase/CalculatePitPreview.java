package com.mateuszcer.taxbackend.pit.domain.usecase;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.capitalgains.domain.OrderSnapshot;
import com.mateuszcer.taxbackend.capitalgains.domain.port.UserOrdersProvider;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;
import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;
import com.mateuszcer.taxbackend.pit.domain.service.CurrencyConversionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalculatePitPreview {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    
    private final UserOrdersProvider ordersProvider;
    private final CurrencyConversionService currencyConversionService;

    public CalculatePitPreview(UserOrdersProvider ordersProvider, 
                               CurrencyConversionService currencyConversionService) {
        this.ordersProvider = ordersProvider;
        this.currencyConversionService = currencyConversionService;
    }

    public PitPreview execute(PitPreviewQuery query) {
        List<String> warnings = new ArrayList<>();
        List<OrderSnapshot> all = ordersProvider.getForUser(query.userId());
        List<OrderSnapshot> orders = all.stream()
                .filter(o -> o != null)
                .filter(o -> o.occurredAt() != null)
                .filter(o -> ZonedDateTime.ofInstant(o.occurredAt(), ZONE).getYear() == query.taxYear())
                .filter(o -> o.status() != null && o.status().equalsIgnoreCase("FILLED"))
                .sorted(Comparator.comparing(OrderSnapshot::occurredAt))
                .toList();

        Map<String, Deque<Lot>> lotsByProduct = new HashMap<>();

        BigDecimal totalCostPln = BigDecimal.ZERO;
        BigDecimal totalProceedsPln = BigDecimal.ZERO;

        for (OrderSnapshot o : orders) {
            String side = o.side() == null ? "" : o.side().toUpperCase(Locale.ROOT);
            LocalDate transactionDate = LocalDate.ofInstant(o.occurredAt(), ZONE);
            String currency = extractCurrency(o.productId());
            
            if (side.equals("BUY")) {
                BigDecimal qty = nz(o.quantity());
                if (qty.signum() <= 0) {
                    continue;
                }
                BigDecimal unitCostUsd = computeBuyUnitCost(o);
                BigDecimal unitCostPln = currencyConversionService.convertToPln(unitCostUsd, currency, transactionDate);
                
                lotsByProduct.computeIfAbsent(nzStr(o.productId()), k -> new ArrayDeque<>())
                        .addLast(new Lot(qty, unitCostPln));
            } else if (side.equals("SELL")) {
                BigDecimal qtyToSell = nz(o.quantity());
                if (qtyToSell.signum() <= 0) {
                    continue;
                }

                BigDecimal proceedsUsd = computeSellProceeds(o);
                BigDecimal proceedsPln = currencyConversionService.convertToPln(proceedsUsd, currency, transactionDate);
                totalProceedsPln = totalProceedsPln.add(proceedsPln);

                String productId = nzStr(o.productId());
                Deque<Lot> lots = lotsByProduct.computeIfAbsent(productId, k -> new ArrayDeque<>());

                BigDecimal cost = BigDecimal.ZERO;
                BigDecimal remaining = qtyToSell;

                while (remaining.signum() > 0 && !lots.isEmpty()) {
                    Lot lot = lots.peekFirst();
                    BigDecimal take = remaining.min(lot.qtyRemaining);
                    cost = cost.add(take.multiply(lot.unitCostPln));
                    lot.qtyRemaining = lot.qtyRemaining.subtract(take);
                    remaining = remaining.subtract(take);
                    if (lot.qtyRemaining.signum() == 0) {
                        lots.removeFirst();
                    }
                }

                if (remaining.signum() > 0) {
                    warnings.add("Missing buy lots for product " + productId + " (remaining " + remaining + ")");
                }

                totalCostPln = totalCostPln.add(cost);
            }
        }

        totalCostPln = totalCostPln.max(BigDecimal.ZERO);
        totalProceedsPln = totalProceedsPln.max(BigDecimal.ZERO);

        BigDecimal gainPln = totalProceedsPln.subtract(totalCostPln);

        return new PitPreview(
                query.taxYear(),
                scale(totalCostPln),
                scale(totalProceedsPln),
                scale(gainPln),
                warnings
        );
    }
    
    /**
     * Extract currency from product ID (e.g., "BTC-USD" -> "USD", "ETH-EUR" -> "EUR")
     */
    private String extractCurrency(String productId) {
        if (productId == null || productId.isEmpty()) {
            return "USD"; // Default to USD
        }
        
        String[] parts = productId.split("-");
        if (parts.length >= 2) {
            String currency = parts[1];
            // Handle USDC/USDT as USD for exchange rate purposes
            if ("USDC".equals(currency) || "USDT".equals(currency)) {
                return "USD";
            }
            return currency;
        }
        
        return "USD"; // Default
    }

    private static BigDecimal computeBuyUnitCost(OrderSnapshot o) {
        BigDecimal qty = nz(o.quantity());
        BigDecimal total = nz(o.total());
        BigDecimal fee = nz(o.fee());
        if (qty.signum() > 0 && (total.signum() > 0 || fee.signum() > 0)) {
            return total.add(fee).divide(qty, 18, RoundingMode.HALF_UP);
        }
        BigDecimal price = nz(o.price());
        if (price.signum() > 0) {
            return price;
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal computeSellProceeds(OrderSnapshot o) {
        BigDecimal total = nz(o.total());
        BigDecimal fee = nz(o.fee());
        if (total.signum() > 0) {
            return total.subtract(fee);
        }
        BigDecimal qty = nz(o.quantity());
        BigDecimal price = nz(o.price());
        if (qty.signum() > 0 && price.signum() > 0) {
            return qty.multiply(price).subtract(fee);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String nzStr(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class Lot {
        private BigDecimal qtyRemaining;
        private final BigDecimal unitCostPln;

        private Lot(BigDecimal qtyRemaining, BigDecimal unitCostPln) {
            this.qtyRemaining = qtyRemaining;
            this.unitCostPln = unitCostPln;
        }
    }
}

