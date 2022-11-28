package uk.co.kenfos

import java.time.Instant

typealias Dictionary<KEY, VALUE> = LastWriteWinsElementDictionary<KEY, VALUE>

data class WithInstant<VALUE>(val value: VALUE, val instant: Instant)

class LastWriteWinsElementDictionary<KEY, VALUE>(
    val added: MutableMap<KEY, WithInstant<VALUE>> = LinkedHashMap(),
    val removed: MutableMap<KEY, Instant> = LinkedHashMap()
) {
    fun add(key: KEY, value: VALUE, instant: Instant): Dictionary<KEY, VALUE> {
        val item = added[key]
        val newItem = Pair(key, WithInstant(value, instant))
        val validAdd = item == null || item.instant.isBefore(instant) || priorityInConflict(item, newItem.second)
        if (validAdd) added[key] = WithInstant(value, instant)
        return this
    }

    fun remove(key: KEY, instant: Instant): Dictionary<KEY, VALUE> {
        val item = added[key]
        val itemExists = item != null
        if (itemExists) removed[key] = instant
        return this
    }

    fun update(key: KEY, value: VALUE, instant: Instant): Dictionary<KEY, VALUE> {
        val item = added[key]
        val validUpdate = item != null
        return if (validUpdate) return this.add(key, value, instant) else this
    }

    fun lookup(key: KEY): VALUE? {
        val item = added[key]
        val removedItem = removed[key]
        val activeItem = removedItem == null || removedItem.isBefore(item?.instant)
        return if (activeItem) item?.value else null
    }

    private fun priorityInConflict(item1: WithInstant<VALUE>, item2: WithInstant<VALUE>): Boolean {
        return if (item1.instant == item2.instant) item1.value.hashCode() < item2.value.hashCode() else false
    }

    companion object {
        @JvmStatic
        fun <KEY, VALUE> merge(dictionaries: List<Dictionary<KEY, VALUE>>): Dictionary<KEY, VALUE> {
            val addedEntries = dictionaries.flatMap { dictionary -> dictionary.added.entries }
            val removedEntries = dictionaries.flatMap { dictionary -> dictionary.removed.entries }
            return addedEntries
                .fold(Dictionary<KEY, VALUE>()) { result, (key, item) -> result.add(key, item.value, item.instant) }
                .let { added -> removedEntries.fold(added) { result, entry -> result.remove(entry.key, entry.value) } }
        }
    }
}