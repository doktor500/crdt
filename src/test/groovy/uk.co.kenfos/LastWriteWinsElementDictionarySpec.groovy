package uk.co.kenfos

import spock.lang.Specification

import static uk.co.kenfos.utils.DictionaryUtils.dictionaryFrom

class LastWriteWinsElementDictionarySpec extends Specification {
    def 'can add key-value pairs to the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"
        dictionary.lookup("B") == "1"

        where:
        node1 =
        """                                          
            OPERATION:ADD KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD KEY:B VALUE:1 TIMESTAMP:1
        """
    }

    def 'when adding a key-value pair multiple times the last add operation wins'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "A1"

        where:
        node1 =
        """
            OPERATION:ADD KEY:A VALUE:A0 TIMESTAMP:0
            OPERATION:ADD KEY:A VALUE:A1 TIMESTAMP:1
        """
    }

    def 'can remove a key-value pair from the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        !dictionary.lookup("A")

        where:
        node1 =
        """                                          
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'ignores removing a key-value pair that does not yet exist'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:REMOVE KEY:A         TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
        """
    }

    def 'includes a key-value pair in the dictionary if it is added again after being removed'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
        """
    }

    def 'does not include a key-value pair in the dictionary if it is added, removed, added and removed again'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        !dictionary.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:3
        """
    }

    def 'allows to update the value of an existing key'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "1"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:1 TIMESTAMP:1
        """
    }

    def 'ignores updating the value of a key that is not found'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        !dictionary.lookup("A")

        where:
        node1 =
        """
            OPERATION:UPDATE KEY:A VALUE:0 TIMESTAMP:0
        """
    }

    def 'does not allow to update the value of a non yet existing key'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
            OPERATION:UPDATE KEY:A VALUE:1 TIMESTAMP:0
        """
    }

    def 'an ADD event coming out of order does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'an UPDATE event coming out of order does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "2"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:UPDATE KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:ADD    KEY:A VALUE:1 TIMESTAMP:1
        """
    }

    def 'a REMOVE event coming out of order does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        !dictionary.lookup("A")

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:2
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:1
        """
    }

    def 'an ADD received multiple times does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "2"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:ADD    KEY:A VALUE:2 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
        """
    }

    def 'a REMOVE message received multiple times does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "0"

        where:
        node1 =
        """
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:0
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
            OPERATION:ADD    KEY:A VALUE:0 TIMESTAMP:2
            OPERATION:REMOVE KEY:A         TIMESTAMP:1
        """
    }

    def 'an UPDATE message received multiple times does not affect the computed state of the dictionary'() {
        given:
        def dictionary = dictionaryFrom(node1)

        expect:
        dictionary.lookup("A") == "3"

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

    def 'can merge dictionaries'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)
        def dictionary3 = dictionaryFrom(node3)
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

    def 'last added value wins when merging multiple dictionaries'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)
        def mergedDictionary = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])

        expect:
        mergedDictionary.lookup("A") == "0"
        mergedDictionary.lookup("B") == "2"

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

    def 'merge is commutative'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)
        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary ->
            assert dictionary.lookup("A") == "2"
            assert dictionary.lookup("B") == "5"
            assert !dictionary.lookup("C")
        }

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

    def 'merge is associative'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)
        def dictionary3 = dictionaryFrom(node3)

        def partiallyMergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def partiallyMergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary3])

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([partiallyMergedDictionary1, dictionary3])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary1, partiallyMergedDictionary2])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary ->
            assert dictionary.lookup("A") == "3"
            assert dictionary.lookup("B") == "7"
            assert !dictionary.lookup("C")
        }

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

    def 'merge is idempotent'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([mergedDictionary1, dictionary1, dictionary2])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary ->
            assert dictionary.lookup("A") == "3"
            assert dictionary.lookup("B") == "7"
            assert !dictionary.lookup("C")
        }

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
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary -> assert !dictionary.lookup("A") }

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
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary -> assert !dictionary.lookup("A") }

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

    def 'conflict between ADD + ADD is handled'() {
        given:
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary -> assert dictionary.lookup("A") == "1" }

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
        def dictionary1 = dictionaryFrom(node1)
        def dictionary2 = dictionaryFrom(node2)

        def mergedDictionary1 = LastWriteWinsElementDictionary.merge([dictionary1, dictionary2])
        def mergedDictionary2 = LastWriteWinsElementDictionary.merge([dictionary2, dictionary1])

        expect:
        [mergedDictionary1, mergedDictionary2].each { dictionary -> assert dictionary.lookup("A") == "Y" }

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
}