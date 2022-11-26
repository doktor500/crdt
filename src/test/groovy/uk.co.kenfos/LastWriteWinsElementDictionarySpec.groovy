package uk.co.kenfos

import spock.lang.Specification

import static java.lang.System.lineSeparator

class LastWriteWinsElementDictionarySpec extends Specification {
    private WHITE_SPACE = " ";
    private SEPARATOR = ":";

    def 'can add key-value pairs to the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"
        dictionary1.lookup("B") == "1"

        where:
        node1 =
        """                                          
            OPERATION:ADD KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD KEY:B VALUE:1 TIMESTAMP:1
        """
    }

    def 'when adding a key-value pair multiple times the last add operation wins'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "A1"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:A0 TIMESTAMP:0
            OPERATION:ADD KEY:A VALUE:A1 TIMESTAMP:1
        """
    }

    def 'can remove a key-value pair from the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        !dictionary1.lookup("A")

        where:
        node1 =
        """                                          
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'ignores removing a key-value pair that does not yet exist'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:REMOVE KEY:A         TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
        """
    }

    def 'includes a key-value pair in the dictionary if it is added again after being removed'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
        """
    }

    def 'does not include a key-value pair in the dictionary if it is added, removed, added and removed again'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        !dictionary1.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:3
        """
    }

    def 'allows to update the value of an existing key'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "1"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:1 TIMESTAMP:1
        """
    }

    def 'ignores updating the value of a key that is not found'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        !dictionary1.lookup("A")

        where:
        node1 =
        """
            OPERATION:UPDATE KEY:A VALUE:0 TIMESTAMP:0
        """
    }

    def 'does not allow to update the value of a non yet existing key'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
            OPERATION:UPDATE KEY:A VALUE:1 TIMESTAMP:0
        """
    }

    def 'an ADD event coming out of order does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'an UPDATE event coming out of order does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "2"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
        """
    }

    def 'a REMOVE event coming out of order does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        !dictionary1.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:2
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
        """
    }

    def 'an ADD received multiple times does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "2"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
        """
    }

    def 'a REMOVE message received multiple times does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'an UPDATE message received multiple times does not affect the computed state of the dictionary'(node1) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))

        expect:
        dictionary1.lookup("A") == "3"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:UPDATE KEY:A VALUE:3 TIMESTAMP:3
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
            OPERATION:UPDATE KEY:A VALUE:2 TIMESTAMP:2
        """
    }

    def 'can merge dictionaries'(node1, node2, node3) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))
        def dictionary3 = dictionaryFrom(parse(node3))
        def dictionary = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2, dictionary3])

        expect:
        dictionary.lookup("A") == "0"
        dictionary.lookup("B") == "1"
        dictionary.lookup("C") == "2"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:0 TIMESTAMP:0
        """

        node2 =
        """
            OPERATION:ADD KEY:B VALUE:1 TIMESTAMP:1
        """

        node3 =
        """
            OPERATION:ADD KEY:C VALUE:2 TIMESTAMP:2
        """
    }

    def 'last added value wins when merging multiple dictionaries'(node1, node2) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))
        def dictionary = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])

        expect:
        dictionary.lookup("A") == "0"
        dictionary.lookup("B") == "2"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD KEY:B VALUE:1 TIMESTAMP:1
        """

        node2 =
        """
            OPERATION:ADD KEY:B VALUE:1 TIMESTAMP:1
            OPERATION:ADD KEY:B VALUE:2 TIMESTAMP:2
        """
    }

    def 'merge is commutative'(node1, node2) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))
        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        mergedDictionary1.lookup("A") == "2"
        mergedDictionary1.lookup("B") == "5"
        !mergedDictionary1.lookup("C")

        and:
        mergedDictionary2.lookup("A") == "2"
        mergedDictionary2.lookup("B") == "5"
        !mergedDictionary2.lookup("C")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
            OPERATION:ADD    KEY:B VALUE:2 TIMESTAMP:2
            OPERATION:UPDATE KEY:B VALUE:3 TIMESTAMP:3
            OPERATION:ADD    KEY:C VALUE:4 TIMESTAMP:4
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:ADD    KEY:B VALUE:4 TIMESTAMP:4
            OPERATION:UPDATE KEY:B VALUE:5 TIMESTAMP:5
            OPERATION:ADD    KEY:C VALUE:6 TIMESTAMP:6
            OPERATION:REMOVE KEY:C         TIMESTAMP:7
        """
    }

    def 'merge is associative'(node1, node2, node3) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))
        def dictionary3 = dictionaryFrom(parse(node3))

        def partiallyMergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def partiallyMergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary3])

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([partiallyMergedDictionary1, dictionary3])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary1, partiallyMergedDictionary2])

        expect:
        mergedDictionary1.lookup("A") == "3"
        mergedDictionary1.lookup("B") == "7"
        !mergedDictionary1.lookup("C")

        and:
        mergedDictionary2.lookup("A") == "3"
        mergedDictionary2.lookup("B") == "7"
        !mergedDictionary2.lookup("C")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:B VALUE:2 TIMESTAMP:2
            OPERATION:UPDATE KEY:B VALUE:3 TIMESTAMP:3
            OPERATION:ADD    KEY:C VALUE:4 TIMESTAMP:4
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
            OPERATION:ADD    KEY:B VALUE:4 TIMESTAMP:4
            OPERATION:UPDATE KEY:B VALUE:5 TIMESTAMP:5
            OPERATION:ADD    KEY:C VALUE:6 TIMESTAMP:6
            OPERATION:REMOVE KEY:C         TIMESTAMP:9
        """

        node3 =
        """
            OPERATION:ADD    KEY:A VALUE:3 TIMESTAMP:3
            OPERATION:ADD    KEY:B VALUE:4 TIMESTAMP:4
            OPERATION:UPDATE KEY:B VALUE:7 TIMESTAMP:7
            OPERATION:ADD    KEY:C VALUE:8 TIMESTAMP:8
        """
    }

    def 'merge is idempotent'(node1, node2) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([mergedDictionary1, dictionary1, dictionary2])

        expect:
        mergedDictionary1.lookup("A") == "3"
        mergedDictionary1.lookup("B") == "7"
        !mergedDictionary1.lookup("C")

        and:
        mergedDictionary2.lookup("A") == "3"
        mergedDictionary2.lookup("B") == "7"
        !mergedDictionary2.lookup("C")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
            OPERATION:ADD    KEY:B VALUE:2 TIMESTAMP:2
            OPERATION:UPDATE KEY:B VALUE:3 TIMESTAMP:3
            OPERATION:UPDATE KEY:B VALUE:7 TIMESTAMP:7
            OPERATION:ADD    KEY:C VALUE:8 TIMESTAMP:8
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:ADD    KEY:A VALUE:3 TIMESTAMP:3
            OPERATION:ADD    KEY:B VALUE:4 TIMESTAMP:4
            OPERATION:UPDATE KEY:B VALUE:5 TIMESTAMP:5
            OPERATION:UPDATE KEY:B VALUE:6 TIMESTAMP:6
            OPERATION:ADD    KEY:C VALUE:7 TIMESTAMP:7
            OPERATION:REMOVE KEY:C         TIMESTAMP:9
            OPERATION:REMOVE KEY:C         TIMESTAMP:9
        """
    }

    def 'bias between ADD + REMOVE is handled'() {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        !mergedDictionary1.lookup("A")

        and:
        !mergedDictionary2.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:1 TIMESTAMP:1
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'bias between UPDATE + REMOVE is handled'() {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        !mergedDictionary1.lookup("A")

        and:
        !mergedDictionary2.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:1 TIMESTAMP:1
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'conflict between ADD + ADD is handled'(node1, node2) {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        mergedDictionary1.lookup("A") == "1"

        and:
        mergedDictionary2.lookup("A") == "1"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:0 TIMESTAMP:0
        """

        node2 =
        """
            OPERATION:ADD KEY:A VALUE:1 TIMESTAMP:0
        """
    }

    def 'conflict between ADD + UPDATE is handled'() {
        given:
        def dictionary1 = dictionaryFrom(parse(node1))
        def dictionary2 = dictionaryFrom(parse(node2))

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        mergedDictionary1.lookup("A") == "Y"

        and:
        mergedDictionary2.lookup("A") == "Y"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:X TIMESTAMP:1
        """

        node2 =
        """
            OPERATION:ADD    KEY:A VALUE:X TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:Y TIMESTAMP:1
        """
    }

    def dictionaryFrom(List<Event> events) {
        def emptyDictionary = new LastWriteWinsElementDictionary<String, String>()
        events.inject(emptyDictionary) { dictionary, event ->
            if (event.type == EventType.ADD) dictionary.add(event.key, event.value, event.timestamp)
            else if (event.type == EventType.REMOVE) dictionary.remove(event.key, event.timestamp)
            else if (event.type == EventType.UPDATE) dictionary.update(event.key, event.value, event.timestamp)
            else dictionary
        }
    }

    def parse(String text) {
        text.split(lineSeparator()).toList().collect { it.trim() }.findAll { it }.collect { parseLine(it) }
    }

    def parseLine(String line) {
        def type = line.takeBetween("OPERATION${SEPARATOR}", WHITE_SPACE)
        def key = line.takeBetween("KEY${SEPARATOR}", WHITE_SPACE)
        def value = line.takeBetween("VALUE${SEPARATOR}", WHITE_SPACE)
        def timestamp = line.takeAfter("TIMESTAMP${SEPARATOR}")

        new Event(type: parseEventType(type), key: key, value: value, timestamp: parseTimeStamp(timestamp))
    }

    def parseEventType = { String eventType -> EventType.values().find { it.toString().startsWith(eventType) } }
    def parseTimeStamp = { String timestamp -> new Date(timestamp.toInteger()) }
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