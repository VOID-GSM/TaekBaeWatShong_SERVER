package com.example.taekbaewatshongserver.domain.admin.repository

import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCodeUsage
import org.springframework.data.jpa.repository.JpaRepository

interface AdminInviteCodeUsageRepository : JpaRepository<AdminInviteCodeUsage, Long>
