package cash.p.terminal.modules.contacts

import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ContactsRepository {

    private val contactsMap = mutableMapOf(
        "1" to Contact(
            "1",
            "Donate",
            listOf(
                ContactAddress(Blockchain(BlockchainType.Bitcoin, "Bitcoin", null), "3G5fwc9PP9Lcb1y3RAYGzoQZs5enJkmdxN"),
                ContactAddress(Blockchain(BlockchainType.Ethereum, "Ethereum", null), "0x696Ed8f9E2b3265Abc24a6A035d6c5094f61e61B"),
            )
        ),
        "2" to Contact("2", "Bob", listOf()),
        "3" to Contact("3", "John", listOf(
            ContactAddress(Blockchain(BlockchainType.Ethereum, "Ethereum", null), "0xae090025857b9c7b24387741f120538e928a3a12")
        )),
    )

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.toList()

    private val _contactsFlow = MutableStateFlow(contacts)
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow

    fun getContactsFiltered(blockchainType: BlockchainType, query: String): List<Contact> {
        val predicate: (Contact) -> Boolean = {
            if (query.isNotEmpty()) {
                it.name.contains(
                    query,
                    true
                ) && it.addresses.isNotEmpty() && it.addresses.any { it.blockchain.type == blockchainType }
            } else {
                it.addresses.isNotEmpty() && it.addresses.any { it.blockchain.type == blockchainType }
            }
        }

        return contacts.filter(predicate)
    }

    fun save(contact: Contact) {
        contactsMap[contact.id] = contact
        _contactsFlow.update { contacts }
    }

    fun get(id: String): Contact? {
        return contactsMap[id]
    }

    fun delete(id: String) {
        contactsMap.remove(id)
        _contactsFlow.update { contacts }
    }

}