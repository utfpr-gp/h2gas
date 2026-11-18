package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CreateReviewDTO;
import com.h2gas.model.dto.OrderReviewDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Review;
import com.h2gas.model.repository.OrderRepository;
import com.h2gas.model.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// HU24: o cliente avalia a loja e o entregador depois da entrega
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderReviewDTO reviewOrder(String orderId, String customerId, CreateReviewDTO dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenResourceException("Este pedido não é seu.");
        }
        // RN04: só pedido entregue
        if (order.getStatus() != OrderStatusEnum.DELIVERED) {
            throw new BusinessRuleException("Só dá para avaliar um pedido entregue.");
        }
        if (dto.delivererRating() != null && order.getDeliverer() == null) {
            throw new BusinessRuleException("Este pedido não teve entregador para avaliar.");
        }
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já foi avaliado.");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setSupplier(order.getSupplier());
        review.setDeliverer(order.getDeliverer());
        review.setRating(dto.storeRating());
        review.setDelivererRating(dto.delivererRating());
        review.setComment(dto.comment() == null || dto.comment().isBlank() ? null : dto.comment().trim());
        return toDto(reviewRepository.save(review));
    }

    static OrderReviewDTO toDto(Review review) {
        return new OrderReviewDTO(review.getRating(), review.getDelivererRating(), review.getComment());
    }
}
