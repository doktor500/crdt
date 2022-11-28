package uk.co.kenfos

typealias Dictionary<K, V, T> = LastWriteWinsElementDictionary<K, V, T>

data class WithTimestamp<V, T>(val value: V, val timestamp: T)

class LastWriteWinsElementDictionary<K, V, T>(
    val added: MutableMap<K, WithTimestamp<V, T>> = LinkedHashMap(),
    val removed: MutableMap<K, T> = LinkedHashMap()
) where T : Comparable<T> {
    fun add(key: K, value: V, timestamp: T): Dictionary<K, V, T> {
        val item = added[key]
        val newItem = Pair(key, WithTimestamp(value, timestamp))
        val validAdd = item == null || item.timestamp < timestamp || priorityInConflict(item, newItem.second)
        if (validAdd) added[key] = WithTimestamp(value, timestamp)
        return this
    }

    fun remove(key: K, timestamp: T): Dictionary<K, V, T> {
        val item = added[key]
        val itemExists = item != null
        if (itemExists) removed[key] = timestamp
        return this
    }

    fun update(key: K, value: V, timestamp: T): Dictionary<K, V, T> {
        val item = added[key]
        val validUpdate = item != null
        return if (validUpdate) return this.add(key, value, timestamp) else this
    }

    fun lookup(key: K): V? {
        val item = added[key]
        val removedItem = removed[key]
        val activeItem = removedItem == null || (item != null && removedItem < item.timestamp)
        return if (activeItem) item?.value else null
    }

    private fun priorityInConflict(item1: WithTimestamp<V, T>, item2: WithTimestamp<V, T>): Boolean {
        return if (item1.timestamp == item2.timestamp) item1.value.hashCode() < item2.value.hashCode() else false
    }

    companion object {
        @JvmStatic
        fun <K, V, T> merge(vararg dictionaries: Dictionary<K, V, T>): Dictionary<K, V, T> where T : Comparable<T> {
            val addedEntries = dictionaries.flatMap { dictionary -> dictionary.added.entries }
            val removedEntries = dictionaries.flatMap { dictionary -> dictionary.removed.entries }
            return addedEntries
                .fold(Dictionary<K, V, T>()) { result, (key, item) -> result.add(key, item.value, item.timestamp) }
                .let { added -> removedEntries.fold(added) { result, entry -> result.remove(entry.key, entry.value) } }
        }
    }
}