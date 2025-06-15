package com.petfoodstore.repository;

import com.petfoodstore.entity.Notification;
import com.petfoodstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUser(@Param("user") User user);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    Long countUnreadNotificationsByUser(@Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotificationsByUser(@Param("user") User user, @Param("limit") int limit);

    @Query("SELECT n FROM Notification n WHERE n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByType(@Param("type") Notification.NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndType(@Param("user") User user, @Param("type") Notification.NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.order.id = :orderId ORDER BY n.createdAt DESC")
    List<Notification> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :start AND :end ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    @Query("SELECT n FROM Notification n WHERE n.user.role IN :roles ORDER BY n.createdAt DESC")
    List<Notification> findByUserRoles(@Param("roles") List<com.petfoodstore.entity.User.UserRole> roles);
}
