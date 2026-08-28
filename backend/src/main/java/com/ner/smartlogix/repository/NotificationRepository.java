package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFlagFalseOrderBySentAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFlagFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readFlag = true WHERE n.user.id = :userId AND n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}
