package cash.p.terminal.core.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CexDepositNetworkRaw
import cash.p.terminal.core.providers.CexWithdrawNetworkRaw
import cash.p.terminal.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import java.util.Date

class DatabaseConverters {

    private val gson by lazy { Gson() }

    // BigDecimal

    @TypeConverter
    fun fromString(value: String?): BigDecimal? = try {
        value?.let { BigDecimal(it) }
    } catch (e: Exception) {
        null
    }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toPlainString()
    }

    // SecretString

    @TypeConverter
    fun decryptSecretString(value: String?): SecretString? {
        if (value == null) return null

        return try {
            SecretString(App.encryptionManager.decrypt(value))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretString(secretString: SecretString?): String? {
        return secretString?.value?.let { App.encryptionManager.encrypt(it) }
    }

    // SecretList

    @TypeConverter
    fun decryptSecretList(value: String?): SecretList? {
        if (value == null) return null

        return try {
            SecretList(App.encryptionManager.decrypt(value).split(","))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretList(secretList: SecretList?): String? {
        return secretList?.list?.joinToString(separator = ",")?.let {
            App.encryptionManager.encrypt(it)
        }
    }

    @TypeConverter
    fun fromWCPeerMeta(peerMeta: WCPeerMeta): String {
        return gson.toJson(peerMeta)
    }

    @TypeConverter
    fun toWCPeerMeta(json: String): WCPeerMeta {
        return gson.fromJson(json, WCPeerMeta::class.java)
    }

    @TypeConverter
    fun fromWCSession(session: WCSession): String {
        return gson.toJson(session)
    }

    @TypeConverter
    fun toWCSession(json: String): WCSession {
        return gson.fromJson(json, WCSession::class.java)
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun fromBlockchainType(blockchainType: BlockchainType): String {
        return blockchainType.uid
    }

    @TypeConverter
    fun toBlockchainType(string: String): BlockchainType {
        return BlockchainType.fromUid(string)
    }

    @TypeConverter
    fun fromNftUid(nftUid: NftUid): String {
        return nftUid.uid
    }

    @TypeConverter
    fun toNftUid(string: String): NftUid {
        return NftUid.fromUid(string)
    }

    @TypeConverter
    fun fromCexDepositNetworkList(networks: List<CexDepositNetworkRaw>): String {
        return gson.toJson(networks)
    }

    @TypeConverter
    fun toCexDepositNetworkList(json: String): List<CexDepositNetworkRaw>? {
        return gson.fromJson(
            json,
            object : TypeToken<List<CexDepositNetworkRaw>>() {}.type
        )
    }

    @TypeConverter
    fun fromCexWithdrawNetworkList(networks: List<CexWithdrawNetworkRaw>): String {
        return gson.toJson(networks)
    }

    @TypeConverter
    fun toCexWithdrawNetworkList(json: String): List<CexWithdrawNetworkRaw>? {
        return gson.fromJson(
            json,
            object : TypeToken<List<CexWithdrawNetworkRaw>>() {}.type
        )
    }
}
