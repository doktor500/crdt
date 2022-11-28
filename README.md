### Introduction

Conflict Free Replicated Data Types (CRDTs) are data structures that power real time collaborative applications in
distributed systems. CRDTs can be replicated across systems, they can be updated independently and concurrently without
coordination between the replicas, and it is always mathematically possible to resolve inconsistencies which might
result

### Recommended Reading

- https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type
- https://github.com/pfrazee/crdt_notes
- https://hal.inria.fr/inria-00555588/PDF/techreport.pdf

### Deliverable

Study LWW-Element-Set and implement a state-based LWW-Element-Dictionary with test cases. Similar to LWW-Element-Set,
the dictionary variant you are going to implement will:

- Store a timestamp for each key-value pair
- Lookup, add, and remove operations
- Allow updating the value of a key
- There should be a function to merge two dictionaries
- Test cases should be clearly written and document what aspect of CRDT they test.

### Implementation notes

- I decided to use a generic type for the timestamp which implements `Comparable` so objects such as `Instant`, `Date`,
  `Timestamp`, etc. can be used
- Although I started using a Functional Programming style, in the end, I made the internal state of the dictionary
  mutable since it makes more efficient usage of the memory space
- The function to merge dictionaries is more abstract and allows to merge more than two dictionaries if needed
- Optimisations to clean up historical changes that become non-relevant are not included, for instance, if an item `A`
  is added, then removed and then added back again, the full history of events is retained
- Events coming out of order or more than once are handled so that the state of the dictionary converges
- The merge operation is commutative, associative and idempotent
- Bias between an `ADD` and a `REMOVE` operation has been resolved so that the `REMOVE` operation takes priority
- Bias between an `UPDATE` and a `REMOVE` operation has been resolved so that the `REMOVE` operation takes priority
- When there is a conflict (multiple events happening at the exact same instant) between two `ADD` operations or
  an `ADD` and an `UPDATE` operation, the item with the lowest hashcode is retained, with the aim to provide a
  deterministic implementation to handle such conflicts
- This solution makes the assumption that the size of these dictionaries can be stored in memory  