package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.CrmContact
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsMemoryCache @Inject constructor() {
    private val contactsByTenant = mutableMapOf<String, List<CrmContact>>()

    fun contactsFor(tenantId: String): List<CrmContact>? {
        return synchronized(contactsByTenant) {
            contactsByTenant[tenantId]
        }
    }

    fun contactFor(tenantId: String, contactId: String): CrmContact? {
        return synchronized(contactsByTenant) {
            contactsByTenant[tenantId]?.firstOrNull { it.id == contactId }
        }
    }

    fun put(tenantId: String, contacts: List<CrmContact>) {
        synchronized(contactsByTenant) {
            contactsByTenant[tenantId] = contacts
        }
    }

    fun upsert(tenantId: String, contact: CrmContact) {
        synchronized(contactsByTenant) {
            val contacts = contactsByTenant[tenantId].orEmpty()
            contactsByTenant[tenantId] = if (contacts.any { it.id == contact.id }) {
                contacts.map { existing -> if (existing.id == contact.id) contact else existing }
            } else {
                listOf(contact) + contacts
            }
        }
    }

    fun remove(tenantId: String, contactId: String) {
        synchronized(contactsByTenant) {
            contactsByTenant[tenantId] = contactsByTenant[tenantId].orEmpty()
                .filterNot { it.id == contactId }
        }
    }
}
