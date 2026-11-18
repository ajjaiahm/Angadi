package com.angadi.app.data.repository

import androidx.room.withTransaction
import com.angadi.app.data.local.AddressEntity
import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.domain.logic.AddressField
import com.angadi.app.domain.logic.AddressValidator
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.AddressLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SaveAddressResult {
    data class Saved(val id: Long) : SaveAddressResult
    data class Invalid(val errors: Map<AddressField, String>) : SaveAddressResult
}

class AddressRepository(private val db: AngadiDatabase) {
    private val dao get() = db.addressDao()

    val addresses: Flow<List<Address>> = dao.observe().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: Long): Address? = dao.get(id)?.toDomain()

    /** Validates, then inserts or updates. The first address saved automatically becomes the default. */
    suspend fun save(input: Address, makeDefault: Boolean = input.isDefault): SaveAddressResult {
        val cleaned = AddressValidator.clean(input)
        val errors = AddressValidator.validate(cleaned)
        if (errors.isNotEmpty()) return SaveAddressResult.Invalid(errors)
        return db.withTransaction {
            val shouldBeDefault = makeDefault || dao.count() == 0
            val id = if (cleaned.id == 0L) {
                dao.insert(cleaned.toEntity(isDefault = false))
            } else {
                val existing = dao.get(cleaned.id)
                dao.update(cleaned.toEntity(isDefault = existing?.isDefault ?: false))
                cleaned.id
            }
            if (shouldBeDefault) dao.setDefault(id)
            SaveAddressResult.Saved(id)
        }
    }

    suspend fun setDefault(id: Long) = dao.setDefault(id)

    /** Deleting the default address promotes the most recently added remaining one. */
    suspend fun delete(id: Long) = db.withTransaction {
        val wasDefault = dao.get(id)?.isDefault == true
        dao.delete(id)
        if (wasDefault) dao.newestId()?.let { dao.setDefault(it) }
    }
}

private fun AddressEntity.toDomain() = Address(
    id, AddressLabel.entries.firstOrNull { it.name == label } ?: AddressLabel.OTHER,
    recipientName, phone, house, street, area, city, state, pinCode, isDefault,
)

private fun Address.toEntity(isDefault: Boolean) = AddressEntity(id, label.name, recipientName, phone, house, street, area, city, state, pinCode, isDefault)
