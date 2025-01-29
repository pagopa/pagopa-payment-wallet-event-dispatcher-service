package it.pagopa.wallet.eventdispatcher.common.cdc

import java.time.OffsetDateTime

open class LoggingEvent(
    open val id: String,
    open val timestamp: String,
    open val type: String = LoggingEvent::class.java.simpleName
) {}

sealed class WalletLoggingEvent(
    open val walletId: String,
    override val id: String,
    override val timestamp: String,
    override val type: String = WalletLoggingEvent::class.java.simpleName
) : LoggingEvent(id, timestamp)

data class WalletDeletedEvent(
    override val id: String,
    override val timestamp: String,
    override val type: String = WalletDeletedEvent::class.java.simpleName,
    override val walletId: String
) : WalletLoggingEvent(walletId, id, timestamp)

data class WalletApplicationsUpdatedEvent(
    override val id: String,
    override val timestamp: String,
    override val type: String = WalletApplicationsUpdatedEvent::class.java.simpleName,
    override val walletId: String,
    val updatedApplications: List<AuditWalletApplication>
) : WalletLoggingEvent(walletId, id, timestamp)

data class WalletOnboardCompletedEvent(
    override val id: String,
    override val timestamp: String,
    override val type: String = WalletOnboardCompletedEvent::class.java.simpleName,
    override val walletId: String,
    val auditWallet: AuditWallet
) : WalletLoggingEvent(walletId, id, timestamp)

class WarmupLoggingEvent :
    LoggingEvent(
        id = "00000000-0000-0000-0000-000000000000",
        timestamp = OffsetDateTime.now().toString(),
        type = WarmupLoggingEvent::class.java.simpleName
    )
