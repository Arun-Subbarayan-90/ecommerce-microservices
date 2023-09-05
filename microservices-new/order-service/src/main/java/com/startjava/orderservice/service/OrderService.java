package com.startjava.orderservice.service;

import com.startjava.orderservice.dto.InventoryResponse;
import com.startjava.orderservice.dto.OrderLineItemsDto;
import com.startjava.orderservice.dto.OrderRequest;
import com.startjava.orderservice.model.Order;
import com.startjava.orderservice.model.OrderLineItems;
import com.startjava.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItemsList);
       List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                  .uri("http://localhost:8082/api/inventory",
                          uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                  .retrieve()
                  .bodyToMono(InventoryResponse[].class)
                  .block();
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                                    .allMatch((InventoryResponse::isInStock));
        if(allProductsInStock)
            orderRepository.save(order);
        else
            throw new IllegalArgumentException("Product is not in stock, please try again later");
    }
    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
