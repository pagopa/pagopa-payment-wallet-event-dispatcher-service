package it.pagopa.wallet.eventdispatcher.repositories.redis.bean

import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.DeploymentVersionDto

/** Data class that contain all information about a specific event receiver */
data class ReceiversStatus(
    val consumerInstanceId: String,
    val queriedAt: String,
    val version: DeploymentVersionDto?,
    val receiverStatuses: List<ReceiverStatus>
)
