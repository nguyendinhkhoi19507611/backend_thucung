package com.petfoodstore.repository;

import com.petfoodstore.entity.ChatMessage;
import com.petfoodstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT m FROM ChatMessage m WHERE m.ticketId = :ticketId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") String ticketId);

    @Query("SELECT m FROM ChatMessage m WHERE m.order.id = :orderId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByOrderIdOrderByCreatedAtAsc(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = :user AND m.isRead = false")
    Long countUnreadMessagesForUser(@Param("user") User user);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.ticketId = :ticketId AND m.receiver = :user AND m.isRead = false")
    Long countUnreadMessagesInTicket(@Param("ticketId") String ticketId, @Param("user") User user);

    @Query("SELECT m FROM ChatMessage m WHERE m.receiver = :user AND m.isRead = false ORDER BY m.createdAt DESC")
    List<ChatMessage> findUnreadMessagesForUser(@Param("user") User user);

    @Query("SELECT DISTINCT m.ticketId FROM ChatMessage m WHERE " +
            "(m.sender = :user OR m.receiver = :user) AND m.ticketId IS NOT NULL")
    List<String> findTicketIdsByUser(@Param("user") User user);

    @Query("SELECT m FROM ChatMessage m WHERE m.ticketId = :ticketId ORDER BY m.createdAt DESC LIMIT 1")
    ChatMessage findLatestMessageInTicket(@Param("ticketId") String ticketId);

    @Query("SELECT m FROM ChatMessage m WHERE m.createdAt BETWEEN :start AND :end ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m FROM ChatMessage m WHERE m.messageType = :messageType ORDER BY m.createdAt DESC")
    List<ChatMessage> findByMessageType(@Param("messageType") ChatMessage.MessageType messageType);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}