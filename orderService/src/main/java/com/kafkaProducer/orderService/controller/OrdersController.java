package com.kafkaProducer.orderService.controller;

import com.kafkaProducer.orderService.dto.OrderRequestDTO;
import com.kafkaProducer.orderService.dto.OrderResponseDTO;
import com.kafkaProducer.orderService.service.OrderProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
public class OrdersController {

    private final OrderProcessor orderProcessor;
    @PostMapping("/publish")
    public ResponseEntity<OrderResponseDTO> processOrder(OrderRequestDTO orderRequestDTO){
        long order = orderProcessor.processOrder(orderRequestDTO);
        if (order > 0){
            return new ResponseEntity<>(new OrderResponseDTO(order, "Successfully published message"), HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(new OrderResponseDTO(-1L, "Failed to published message"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
