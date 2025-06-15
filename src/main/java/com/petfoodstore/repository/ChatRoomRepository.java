package com.petfoodstore.repository;

import com.petfoodstore.entity.ChatRoom;
import com.petfoodstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomId(String roomId);

    List<ChatRoom> findByCustomerOrderByLastMessageAtDesc(User customer);

    List<ChatRoom> findByStaffOrderByLastMessageAtDesc(User staff);

    @Query("SELECT r FROM ChatRoom r WHERE r.status = :status ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findByStatus(@Param("status") ChatRoom.RoomStatus status);

    @Query("SELECT r FROM ChatRoom r WHERE r.roomType = :roomType ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findByRoomType(@Param("roomType") ChatRoom.RoomType roomType);

    @Query("SELECT r FROM ChatRoom r WHERE r.staff IS NULL AND r.status = :status ORDER BY r.createdAt ASC")
    List<ChatRoom> findUnassignedRoomsByStatus(@Param("status") ChatRoom.RoomStatus status);

    @Query("SELECT r FROM ChatRoom r WHERE " +
            "(r.customer = :user OR r.staff = :user) " +
            "ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findRoomsByUser(@Param("user") User user);

    @Query("SELECT r FROM ChatRoom r WHERE r.priority = :priority ORDER BY r.createdAt ASC")
    List<ChatRoom> findByPriority(@Param("priority") ChatRoom.Priority priority);

    @Query("SELECT COUNT(r) FROM ChatRoom r WHERE r.staff = :staff AND r.status IN :statuses")
    Long countActiveRoomsByStaff(@Param("staff") User staff, @Param("statuses") List<ChatRoom.RoomStatus> statuses);

    @Query("SELECT COUNT(r) FROM ChatRoom r WHERE r.staff IS NULL AND r.status = 'WAITING'")
    Long countUnassignedRooms();
}