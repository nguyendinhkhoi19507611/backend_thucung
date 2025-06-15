package com.petfoodstore.service;

import com.petfoodstore.dto.NotificationDTO;
import com.petfoodstore.entity.Notification;
import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.Product;
import com.petfoodstore.entity.User;
import com.petfoodstore.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WebSocketMessagingService messagingService;

    // Create and send notification
    private Notification createAndSendNotification(User user, String title, String message,
                                                   Notification.NotificationType type,
                                                   String actionUrl, Object relatedEntity) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setActionUrl(actionUrl);

        if (relatedEntity instanceof Order) {
            notification.setOrder((Order) relatedEntity);
        } else if (relatedEntity instanceof Product) {
            notification.setProduct((Product) relatedEntity);
        }

        notification = notificationRepository.save(notification);

        // Send via WebSocket
        NotificationDTO notificationDTO = new NotificationDTO(notification);
        messagingService.sendNotificationToUser(user.getId(), notificationDTO);

        log.info("Notification sent to user {}: {}", user.getUsername(), title);

        return notification;
    }

    // Order notifications
    public void createOrderNotification(Order order, Notification.NotificationType type) {
        String title, message, actionUrl;

        switch (type) {
            case ORDER_CREATED:
                title = "Đơn hàng mới được tạo";
                message = String.format("Đơn hàng #%s đã được tạo thành công", order.getOrderNumber());
                actionUrl = "/orders";
                break;
            case ORDER_STATUS_UPDATED:
                title = "Cập nhật trạng thái đơn hàng";
                message = String.format("Đơn hàng #%s đã được cập nhật trạng thái", order.getOrderNumber());
                actionUrl = "/orders";
                break;
            default:
                return;
        }

        createAndSendNotification(order.getUser(), title, message, type, actionUrl, order);

        // Notify admin/employee about new orders
        if (type == Notification.NotificationType.ORDER_CREATED) {
            notifyAdminUsers("Đơn hàng mới",
                    String.format("Đơn hàng #%s từ %s cần xử lý",
                            order.getOrderNumber(),
                            order.getUser().getFullName() != null ?
                                    order.getUser().getFullName() : order.getUser().getUsername()),
                    "/employee/orders");
        }
    }

    // Payment notifications
    public void createPaymentNotification(Order order, Notification.NotificationType type) {
        String title, message;

        switch (type) {
            case PAYMENT_SUCCESSFUL:
                title = "Thanh toán thành công";
                message = String.format("Thanh toán cho đơn hàng #%s đã thành công", order.getOrderNumber());
                break;
            case PAYMENT_FAILED:
                title = "Thanh toán thất bại";
                message = String.format("Thanh toán cho đơn hàng #%s đã thất bại", order.getOrderNumber());
                break;
            default:
                return;
        }

        createAndSendNotification(order.getUser(), title, message, type, "/orders", order);
    }

    // Chat notifications
    public void createChatNotification(User recipient, User sender, String messageContent, String roomId) {
        String title = String.format("Tin nhắn mới từ %s",
                sender.getFullName() != null ? sender.getFullName() : sender.getUsername());
        String message = messageContent.length() > 50 ?
                messageContent.substring(0, 50) + "..." : messageContent;
        String actionUrl = "/chat/" + roomId;

        createAndSendNotification(recipient, title, message, Notification.NotificationType.NEW_MESSAGE, actionUrl, null);
    }

    // Product low stock notification (for admin)
    public void createLowStockNotification(Product product) {
        String title = "Cảnh báo sắp hết hàng";
        String message = String.format("Sản phẩm %s chỉ còn %d trong kho", product.getName(), product.getQuantity());
        String actionUrl = "/admin/products";

        notifyAdminUsers(title, message, actionUrl);
    }

    // Welcome notification for new users
    public void createWelcomeNotification(User user) {
        String title = "Chào mừng đến với Pet Food Store!";
        String message = "Cảm ơn bạn đã đăng ký. Khám phá các sản phẩm tuyệt vời cho thú cưng của bạn!";
        String actionUrl = "/";

        createAndSendNotification(user, title, message, Notification.NotificationType.WELCOME, actionUrl, null);
    }

    // Get user notifications
    public List<NotificationDTO> getUserNotifications(User user, boolean unreadOnly) {
        List<Notification> notifications;

        if (unreadOnly) {
            notifications = notificationRepository.findUnreadNotificationsByUser(user);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        return notifications.stream()
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }

    // Mark notification as read
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        notification.markAsRead();
        notificationRepository.save(notification);

        log.info("Notification {} marked as read by user {}", notificationId, user.getUsername());
    }

    // Mark all notifications as read
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadNotificationsByUser(user);

        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);

        log.info("All notifications marked as read for user {}", user.getUsername());
    }

    // Get unread count
    public Long getUnreadCount(User user) {
        return notificationRepository.countUnreadNotificationsByUser(user);
    }

    // Delete old notifications (cleanup job)
    public void deleteOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);

        log.info("Deleted notifications older than {} days", daysOld);
    }

    // Notify admin users
    private void notifyAdminUsers(String title, String message, String actionUrl) {
        // This would require UserRepository to get admin users
        // Implementation depends on your user management setup
        log.info("Admin notification: {} - {}", title, message);
    }
}
