### I fell in love with the Scala community because of this:
# want Actor Persistance using Cassandra?
- add the library dependencies  
    akka-persistence-cassandra
    akka-persistence-cassandra-launcher
- add the journal you want to use (cassandra) in application.conf
- enjoy.
    Now your PersistentActor when you use the "persist" method
    will persist themselves onto Cassandra.

# want Sharding?
    - copy the example from https://github.com/akka/akka-samples
    - abstract away the "doOnce" boilerplate with a wrapper
    - profit
    