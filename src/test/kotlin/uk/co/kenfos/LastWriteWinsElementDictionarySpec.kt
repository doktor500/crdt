package uk.co.kenfos

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LastWriteWinsElementDictionarySpec : FunSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    context("single dictionary") {
        val dictionary = LastWriteWinsElementDictionary<String, Int, Int>()

        context("add, update and remove") {
            test("is empty by default") {
                dictionary.lookup("a") shouldBe null
            }

            test("items can be added") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    add("b", 1, timestamp = 1)

                    lookup("a") shouldBe 0
                    lookup("b") shouldBe 1
                }
            }

            test("when adding an item multiple times the last add operation wins") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    add("a", 1, timestamp = 1)

                    lookup("a") shouldBe 1
                }
            }

            test("can remove an item from the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    remove("a", timestamp = 1)

                    lookup("a") shouldBe null
                }
            }

            test("ignores removing an item that does not yet exist") {
                dictionary.apply {
                    remove("a", timestamp = 0)
                    add("a", 1, timestamp = 1)

                    lookup("a") shouldBe 1
                }
            }

            test("includes an item in the dictionary if it is added again after being removed") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    remove("a", timestamp = 1)
                    add("a", 2, timestamp = 2)

                    lookup("a") shouldBe 2
                }
            }

            test("does not include an item in the dictionary if it is added, removed, added and removed again") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    remove("a", timestamp = 1)
                    add("a", 2, timestamp = 2)
                    remove("a", timestamp = 3)

                    lookup("a") shouldBe null
                }
            }

            test("allows to update the value of an existing item") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    update("a", 1, timestamp = 1)

                    lookup("a") shouldBe 1
                }
            }

            test("ignores updating the value of an item that is not found") {
                dictionary.apply {
                    update("a", 0, timestamp = 0)

                    lookup("a") shouldBe null
                }
            }

            test("does not allow to update the value of a non yet existing item") {
                dictionary.apply {
                    add("a", 0, timestamp = 1)
                    update("a", 1, timestamp = 0)

                    lookup("a") shouldBe 0
                }
            }
        }

        context("events coming out of order") {
            test("an ADD event coming out of order does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    add("a", 2, timestamp = 2)
                    remove("a", timestamp = 1)

                    lookup("a") shouldBe 2
                }
            }

            test("an UPDATE event coming out of order does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    update("a", 2, timestamp = 2)
                    add("a", 1, timestamp = 1)

                    lookup("a") shouldBe 2
                }
            }

            test("a REMOVE event coming out of order does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    remove("a", timestamp = 2)
                    add("a", 1, timestamp = 1)

                    lookup("a") shouldBe null
                }
            }
        }

        context("events received multiple times") {
            test("an ADD received multiple times does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    add("a", 2, timestamp = 2)
                    remove("a", timestamp = 1)
                    add("a", 0, timestamp = 0)

                    lookup("a") shouldBe 2
                }
            }

            test("an UPDATE message received multiple times does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    update("a", 2, timestamp = 2)
                    update("a", 3, timestamp = 3)
                    add("a", 1, timestamp = 1)
                    update("a", 2, timestamp = 2)

                    lookup("a") shouldBe 3
                }
            }

            test("a REMOVE message received multiple times does not affect the computed state of the dictionary") {
                dictionary.apply {
                    add("a", 0, timestamp = 0)
                    remove("a", timestamp = 1)
                    add("a", 2, timestamp = 2)
                    remove("a", timestamp = 1)

                    lookup("a") shouldBe 2
                }
            }
        }

        context("merge") {
            val dictionary1 = LastWriteWinsElementDictionary<String, Int, Int>()
            val dictionary2 = LastWriteWinsElementDictionary<String, Int, Int>()
            val dictionary3 = LastWriteWinsElementDictionary<String, Int, Int>()

            test("can merge dictionaries") {
                dictionary1.add("a", 0, timestamp = 0)
                dictionary2.add("b", 1, timestamp = 1)
                dictionary3.add("c", 2, timestamp = 2)

                LastWriteWinsElementDictionary.merge(dictionary1, dictionary2, dictionary3).apply {
                    lookup("a") shouldBe 0
                    lookup("b") shouldBe 1
                    lookup("c") shouldBe 2
                }
            }

            test("the last added value wins when merging multiple dictionaries") {
                dictionary1.apply {
                    add("a", 0, timestamp = 0)
                    add("b", 1, timestamp = 1)
                }

                dictionary2.apply {
                    add("b", 1, timestamp = 1)
                    add("b", 2, timestamp = 2)
                }

                LastWriteWinsElementDictionary.merge(dictionary1, dictionary2).apply {
                    lookup("a") shouldBe 0
                    lookup("b") shouldBe 2
                }
            }

            test("merge is commutative") {
                dictionary1.apply {
                    add("a", 0, timestamp = 0)
                    add("b", 1, timestamp = 1)
                    update("b", 2, timestamp = 2)
                    add("c", 3, timestamp = 3)
                }

                dictionary2.apply {
                    add("a", 0, timestamp = 0)
                    add("a", 1, timestamp = 1)
                    add("b", 3, timestamp = 3)
                    update("b", 4, timestamp = 4)
                    add("c", 5, timestamp = 5)
                    remove("c", timestamp = 6)
                }

                LastWriteWinsElementDictionary.merge(dictionary1, dictionary2).apply {
                    lookup("a") shouldBe 1
                    lookup("b") shouldBe 4
                    lookup("c") shouldBe null
                }
            }

            test("merge is associative") {
                dictionary1.apply {
                    add("a", 0, timestamp = 0)
                    add("b", 2, timestamp = 2)
                    update("b", 3, timestamp = 3)
                    add("c", 4, timestamp = 4)
                }

                dictionary2.apply {
                    add("a", 1, timestamp = 1)
                    add("b", 4, timestamp = 4)
                    update("b", 5, timestamp = 5)
                    add("c", 6, timestamp = 6)
                    remove("c", timestamp = 9)
                }

                dictionary3.apply {
                    add("a", 3, timestamp = 3)
                    add("b", 4, timestamp = 4)
                    update("b", 7, timestamp = 7)
                    add("c", 8, timestamp = 8)
                }

                val partiallyMergedDictionary1 = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                val partiallyMergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary2, dictionary3)

                val mergedDictionary1 = LastWriteWinsElementDictionary.merge(partiallyMergedDictionary1, dictionary3)
                val mergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary1, partiallyMergedDictionary2)

                listOf(mergedDictionary1, mergedDictionary2).forEach { dictionary ->
                    dictionary.apply {
                        lookup("a") shouldBe 3
                        lookup("b") shouldBe 7
                        lookup("c") shouldBe null
                    }
                }
            }

            test("merge is idempotent") {
                dictionary1.apply {
                    add("a", 0, timestamp = 0)
                    add("a", 1, timestamp = 1)
                    add("b", 2, timestamp = 2)
                    update("b", 3, timestamp = 3)
                    update("b", 7, timestamp = 7)
                    add("c", 8, timestamp = 8)
                }

                dictionary2.apply {
                    add("a", 2, timestamp = 2)
                    add("a", 3, timestamp = 3)
                    add("b", 4, timestamp = 4)
                    update("b", 5, timestamp = 5)
                    update("b", 6, timestamp = 6)
                    add("c", 7, timestamp = 7)
                    remove("c", timestamp = 9)
                    remove("c", timestamp = 9)
                }

                val firstMerge = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                val secondMerge = LastWriteWinsElementDictionary.merge(firstMerge, dictionary1, dictionary2)

                listOf(firstMerge, secondMerge).forEach { dictionary ->
                    dictionary.apply {
                        lookup("a") shouldBe 3
                        lookup("b") shouldBe 7
                        lookup("c") shouldBe null
                    }
                }
            }

            context("bias is handled") {
                test("bias between ADD + REMOVE is handled") {
                    dictionary1.add("a", 1, timestamp = 1)

                    dictionary2.apply {
                        add("a", 0, timestamp = 0)
                        remove("a", timestamp = 1)
                    }

                    val mergedDictionary1 = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                    val mergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary2, dictionary1)

                    listOf(mergedDictionary1, mergedDictionary2).forEach { dictionary ->
                        dictionary.lookup("a") shouldBe null
                    }
                }

                test("bias between UPDATE + REMOVE is handled") {
                    dictionary1.apply {
                        add("a", 0, timestamp = 0)
                        update("a", 1, timestamp = 1)
                    }

                    dictionary2.apply {
                        add("a", 0, timestamp = 0)
                        remove("a", timestamp = 1)
                    }

                    val mergedDictionary1 = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                    val mergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary2, dictionary1)

                    listOf(mergedDictionary1, mergedDictionary2).forEach { dictionary ->
                        dictionary.lookup("a") shouldBe null
                    }
                }
            }

            context("conflict is handled") {
                test("conflict between ADD + ADD is handled") {
                    dictionary1.add("a", 0, timestamp = 0)
                    dictionary2.add("a", 1, timestamp = 0)

                    val mergedDictionary1 = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                    val mergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary2, dictionary1)

                    listOf(mergedDictionary1, mergedDictionary2).forEach { dictionary ->
                        dictionary.lookup("a") shouldBe 1
                    }
                }

                test("conflict between ADD + UPDATE is handled") {
                    dictionary1.add("a", 0, timestamp = 1)

                    dictionary2.apply {
                        add("a", 0, timestamp = 0)
                        update("a", 1, timestamp = 1)
                    }

                    val mergedDictionary1 = LastWriteWinsElementDictionary.merge(dictionary1, dictionary2)
                    val mergedDictionary2 = LastWriteWinsElementDictionary.merge(dictionary2, dictionary1)

                    listOf(mergedDictionary1, mergedDictionary2).forEach { dictionary ->
                        dictionary.lookup("a") shouldBe 1
                    }
                }
            }

            test("can merge itself with another dictionary") {
                dictionary1.add("a", 0, timestamp = 0)

                dictionary2.apply {
                    add("b", 1, timestamp = 1)
                    add("c", 2, timestamp = 2)
                    remove("c", timestamp = 3)
                }

                dictionary1.merge(dictionary2).apply {
                    lookup("a") shouldBe 0
                    lookup("b") shouldBe 1
                    lookup("c") shouldBe null
                }
            }
        }
    }
})