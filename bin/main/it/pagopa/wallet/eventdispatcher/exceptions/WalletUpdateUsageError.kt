package it.pagopa.wallet.eventdispatcher.exceptions

import java.util.*

class WalletUpdateUsageError(val walletId: UUID) : Exception()
