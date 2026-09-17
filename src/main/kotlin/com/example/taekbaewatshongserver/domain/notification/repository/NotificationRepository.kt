package com.example.taekbaewatshongserver.domain.notification.repository

import com.example.taekbaewatshongserver.domain.notification.entity.Notification
import com.example.taekbaewatshongserver.domain.notification.entity.NotificationType
import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = ["parcel"])
    fun findByRecipientOrderByIdDesc(recipient: User, pageable: Pageable): List<Notification>

    @EntityGraph(attributePaths = ["parcel"])
    fun findByRecipientAndIdLessThanOrderByIdDesc(recipient: User, id: Long, pageable: Pageable): List<Notification>

    fun countByRecipientAndIsReadFalse(recipient: User): Long

    fun existsByParcelAndTypeAndUnclaimedDays(parcel: Parcel, type: NotificationType, unclaimedDays: Int): Boolean
}
