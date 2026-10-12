package com.example.taekbaewatshongserver.domain.user.repository

import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean

    @Query(
        """
        select u from User u
        where u.id <> :userId
          and (
            u.role in :roles
            or (
              u.role = :dataGsmOnlyRole
              and u.provider = com.example.taekbaewatshongserver.domain.user.entity.AuthProvider.DATAGSM
            )
          )
          and (u.name like :pattern or u.studentNumber like :pattern)
        order by u.name asc, u.id asc
        """,
    )
    fun findChatPartners(
        @Param("userId") userId: Long,
        @Param("roles") roles: Collection<Role>,
        @Param("dataGsmOnlyRole") dataGsmOnlyRole: Role?,
        @Param("pattern") pattern: String,
        pageable: Pageable,
    ): Slice<User>
}
