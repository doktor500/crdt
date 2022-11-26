package uk.co.kenfos

import java.util.*

typealias Timestamp = Date
typealias Dictionary<KEY, VALUE> = LastWriteWinsElementDictionary<KEY, VALUE>
data class WithTimestamp<VALUE>(val value: VALUE, val timestamp: Timestamp)

class LastWriteWinsElementDictionary<KEY, VALUE>(
    val added: Map<KEY, WithTimestamp<VALUE>> = emptyMap(),
    val removed: Map<KEY, Timestamp> = emptyMap()
) {
    fun add(key: KEY, value: VALUE, timestamp: Timestamp): Dictionary<KEY, VALUE> {
        val item = added[key]
        val newItem = Pair(key, WithTimestamp(value, timestamp))
        val validAdd = item == null || item.timestamp.before(timestamp) || priorityInConflict(item, newItem.second)
        return if (validAdd) Dictionary(added.plus(newItem), removed) else this
    }

    fun remove(key: KEY, timestamp: Timestamp): Dictionary<KEY, VALUE> {
        val item = added[key]
        val itemExists = item != null
        return if (itemExists) Dictionary(added, removed.plus(Pair(key, timestamp))) else this
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
            return dictionaries
                .fold(Dictionary<KEY, VALUE>()) { result, current -> mergeAdded(result, current.added) }
                .let { added -> dictionaries.fold(added) { result, current -> mergeRemoved(result, current.removed) } }
        }

        private fun <K, V> mergeAdded(result: Dictionary<K, V>, added: Map<K, WithTimestamp<V>>): Dictionary<K, V> =
            added.entries.fold(result) { dictionary, (key, item) -> dictionary.add(key, item.value, item.timestamp) }

        private fun <K, V> mergeRemoved(result: Dictionary<K, V>, removed: Map<K, Timestamp>): Dictionary<K, V> =
            removed.entries.fold(result) { dictionary, entry -> dictionary.remove(entry.key, entry.value) }
    }
}