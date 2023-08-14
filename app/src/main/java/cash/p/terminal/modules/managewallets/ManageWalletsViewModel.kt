package cash.p.terminal.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.Clearable
import cash.p.terminal.core.badge
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<ConfiguredToken>>>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.configuredToken,
        imageSource = ImageSource.Remote(item.configuredToken.token.coin.imageUrl, item.configuredToken.token.iconPlaceholder),
        title = item.configuredToken.token.coin.code,
        subtitle = item.configuredToken.token.coin.name,
        enabled = item.enabled,
        hasInfo = item.hasInfo,
        label = item.configuredToken.token.badge
    )

    fun enable(configuredToken: ConfiguredToken) {
        service.enable(configuredToken)
    }

    fun disable(configuredToken: ConfiguredToken) {
        service.disable(configuredToken)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    data class BirthdayHeightViewItem(
        val blockchainIcon: ImageSource,
        val blockchainName: String,
        val birthdayHeight: String
    )
}
