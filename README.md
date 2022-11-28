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