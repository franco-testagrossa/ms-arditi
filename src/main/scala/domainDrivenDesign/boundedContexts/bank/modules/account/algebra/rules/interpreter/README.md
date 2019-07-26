# Why AccountRulesV1
## instead of a companion object of AccountRules?
The rules may evolve, and we need to keep track of their evolution

# Why is AccountRulesV1 a trait
## instead of an object, given that it is the interpreter of AccountRules
There is a pivot point: The EventSource trait
We may want to store the events in memory, for testing purposes or we may want to persist the data in a production enviroment
