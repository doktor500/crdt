package uk.co.kenfos.utils

import uk.co.kenfos.LastWriteWinsElementDictionary

import static java.lang.System.lineSeparator

class DictionaryUtils {
    private static WHITE_SPACE = " "
    private static SEPARATOR = ":"

    static dictionaryFrom(String text) {
        def events = parse(text)
        def emptyDictionary = new LastWriteWinsElementDictionary<String, String>()
        events.inject(emptyDictionary) { dictionary, event ->
            if (event.type == EventType.ADD) dictionary.add(event.key, event.value, event.timestamp)
            else if (event.type == EventType.REMOVE) dictionary.remove(event.key, event.timestamp)
            else if (event.type == EventType.UPDATE) dictionary.update(event.key, event.value, event.timestamp)
            else dictionary
        }
    }

    private static parse(String text) {
        text.split(lineSeparator()).toList().collect { it.trim() }.findAll { it }.collect { parseLine(it) }
    }

    private static parseLine(String line) {
        def type = line.takeBetween("OPERATION${SEPARATOR}", WHITE_SPACE)
        def key = line.takeBetween("KEY${SEPARATOR}", WHITE_SPACE)
        def value = line.takeBetween("VALUE${SEPARATOR}", WHITE_SPACE)
        def timestamp = line.takeAfter("TIMESTAMP${SEPARATOR}")

        new Event(type: parseEventType(type), key: key, value: value, timestamp: parseTimeStamp(timestamp))
    }

    private static parseEventType(String eventType) {
        EventType.values().find { it.toString().startsWith(eventType) }
    }

    private static parseTimeStamp(String timestamp) {
        new Date(timestamp.toInteger())
    }
}

class Event {
    EventType type
    String key
    String value
    Date timestamp
}

enum EventType {
    ADD,
    REMOVE,
    UPDATE
}
