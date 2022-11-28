package uk.co.kenfos

import java.util.*
import kotlin.collections.LinkedHashMap

typealias Timestamp = Date
typealias Dictionary<KEY, VALUE> = LastWriteWinsElementDictionary<KEY, VALUE>

data class WithTimestamp<VALUE>(val value: VALUE, val timestamp: Timestamp)

class LastWriteWinsElementDictionary<KEY, VALUE>(
    val added: MutableMap<KEY, WithTimestamp<VALUE>> = LinkedHashMap(),
    val removed: MutableMap<KEY, Timestamp> = LinkedHashMap()
) {
    fun add(key: KEY, value: VALUE, timestamp: Timestamp): Dictionary<KEY, VALUE> {
        val item = added[key]
        val newItem = Pair(key, WithTimestamp(value, timestamp))
        val validAdd = item == null || item.timestamp.before(timestamp) || priorityInConflict(item, newItem.second)
        if (validAdd) added[key] = WithTimestamp(value, timestamp)
        return this
    }

    fun remove(key: KEY, timestamp: Timestamp): Dictionary<KEY, VALUE> {
        val item = added[key]
        val itemExists = item != null
        if (itemExists) removed[key] = timestamp
        return this
    }

    fun update(key: KEY, value: VALUE, timestamp: Timestamp): Dictionary<KEY, VALUE> {
        val item = added[key]
        val validUpdate = item != null
        return if (validUpdate) return this.add(key, value, timestamp) else this
    }

    fun lookup(key: KEY): VALUE? {
        val item = added[key]
        val removedItem = removed[key]
        val activeItem = removedItem == null || removedItem.before(item?.timestamp)
        return if (activeItem) item?.value else null
    }

    private fun priorityInConflict(item1: WithTimestamp<VALUE>, item2: WithTimestamp<VALUE>): Boolean {
        return if (item1.timestamp == item2.timestamp) item1.value.hashCode() < item2.value.hashCode() else false
    }

    companion object {
        @JvmStatic
        fun <KEY, VALUE> merge(dictionaries: List<Dictionary<KEY, VALUE>>): Dictionary<KEY, VALUE> {
            val addedEntries = dictionaries.flatMap { dictionary -> dictionary.added.entries }
            val removedEntries = dictionaries.flatMap { dictionary -> dictionary.removed.entries }
            return addedEntries
                .fold(Dictionary<KEY, VALUE>()) { result, (key, item) -> result.add(key, item.value, item.timestamp) }
                .let { added -> removedEntries.fold(added) { result, entry -> result.remove(entry.key, entry.value) } }
        }
    }
}