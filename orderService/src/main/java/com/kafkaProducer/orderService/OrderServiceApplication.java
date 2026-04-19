package com.kafkaProducer.orderService;

import com.kafkaProducer.orderService.entity.Orders;
import com.kafkaProducer.orderService.repository.OrdersRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class OrderServiceApplication {

    private final OrdersRepository ordersRepository;

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @PostConstruct
    @Transactional
    public void init() {
        log.info("---------populating the DB---------");
         DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
         LocalDateTime date1 = LocalDateTime.parse("2020-11-26 10:35:43", dateTimeFormatter);
         LocalDateTime date2 = LocalDateTime.parse("2020-12-02 17:45:13", dateTimeFormatter);
        List<Orders> orders = List.of(
                new Orders(
                        List.of(
                                "pen",
                                "pencil",
                                "books",
                                "ruler"
                        ),
                        34.6f,
                        date1,
                        "xoxoxoogle@gmail.com"

                ), new Orders(
                        List.of(
                                "Cookies",
                                "Bread",
                                "Sauce",
                                "Apple",
                                "Banana"
                        ),
                        57.34f,
                        date2,
                        "xoxoxoogle@gmail.com"
                )

        );
        try {
            ordersRepository.saveAll(orders);
            log.info("---------successfully populated the DB---------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
