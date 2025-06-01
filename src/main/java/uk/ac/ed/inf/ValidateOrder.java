package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.time.LocalDate;
import java.util.Arrays;

// Define a class ValidateOrder that implements the OrderValidation interface
public class ValidateOrder implements OrderValidation {
    // Implement the validateOrder method from the OrderValidation interface
    @Override
    public Order validateOrder(Order order, Restaurant[] restaurants) {
        Pizza[] pizzaList = order.getPizzasInOrder(); // Get the list of pizzas in the order
        CreditCardInformation cardInfo = order.getCreditCardInformation(); // Get the credit card information
        // Check various validation conditions and set order status and validation code accordingly
        // Validate the maximum number of pizzas per order

        if (pizzaList.length > SystemConstants.MAX_PIZZAS_PER_ORDER) {
            order.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // Validate the CVV of the credit card
        if (!checkIsCVVValid(cardInfo.getCvv())) {
            order.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // Validate the credit card number using Luhn Algorithm
        if (!checkIsCardNumberValid(cardInfo.getCreditCardNumber())) {
            order.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // Check if the credit card is expired
        if (checkIsCardExpired(cardInfo.getCreditCardExpiry(), order.getOrderDate())) {
            order.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // Validate the total cost of the pizzas
        if (order.getPriceTotalInPence() != totalCostOfPizzas(pizzaList)) {
            order.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // Ensure that all pizzas in the order can be fulfilled by one restaurant
        int restaurantCount = 0;
        Restaurant currentRestaurant = null;
        for (Restaurant restaurant : restaurants) {
            int numberOfOrderListPizzasInMenu = findNumberOfOrderListPizzasInMenu(Arrays.stream(pizzaList).toList(), Arrays.stream(restaurant.menu()).toList());
            if (numberOfOrderListPizzasInMenu == pizzaList.length) {
                restaurantCount++;
                currentRestaurant = restaurant;
            } else if (numberOfOrderListPizzasInMenu > 0) {
                restaurantCount++;
            }
        }
        // More validations about the restaurant capabilities and working days
        if (restaurantCount > 1) {
            order.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        } else if (currentRestaurant == null) {
            order.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        } else if (!Arrays.stream(currentRestaurant.openingDays()).toList().contains(order.getOrderDate().getDayOfWeek())) {
            order.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            order.setOrderStatus(OrderStatus.INVALID);
            return order;
        }
        // If all checks pass, the order is marked as valid but not delivered
        order.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        order.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        return order;
    }

    // Helper methods for various checks during order validation
    // Calculate the total cost of pizzas in the order
    private static double totalCostOfPizzas(Pizza[] pizzaList) {
        return Arrays.stream(pizzaList).mapToDouble(Pizza::priceInPence).sum() + SystemConstants.ORDER_CHARGE_IN_PENCE;
    }

    // Check if a CVV is valid (3 digits)
    private static boolean checkIsCVVValid(String CVV) {
        return CVV.matches("[0-9]{3}");
    }

    // Validate a credit card number using Luhn Algorithm
    private static boolean checkIsCardNumberValid(String cardNumber) {
        // Implementation of Luhn Algorithm
        if (!cardNumber.matches("[0-9]{16}")) {
            return false;
        }
        int digitsCount = cardNumber.length();
        int sum = 0;
        for (int i = digitsCount - 1; i >= 0; i--) {
            int temp = cardNumber.charAt(i) - '0';
            if ((digitsCount - i) % 2 == 0)
                temp *=  2;
            sum += temp / 10;
            sum += temp % 10;
        }
        return (sum % 10 == 0);
    }

    // Check if a credit card is expired
    private static boolean checkIsCardExpired(String cardExpiryDate, LocalDate orderDate) {
        int month = Integer.parseInt(cardExpiryDate.charAt(0) + "" + cardExpiryDate.charAt(1));
        int year = Integer.parseInt(cardExpiryDate.charAt(3) + "" + cardExpiryDate.charAt(4));
        if (month > 11) {
            month = 1;
            year += 1;
        } else {
            month += 1;
        }

        return !orderDate.isBefore(LocalDate.parse(String.format("20%02d-%02d-01", year, month)));
    }

    // Find the number of pizzas in the order that are available in the restaurant menu
    public static int findNumberOfOrderListPizzasInMenu(java.util.List<Pizza> orderList, java.util.List<Pizza> menu) {
        return (int) orderList.stream().filter(menu::contains).count();
    }
}
