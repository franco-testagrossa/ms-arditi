# akka cluster on kubernetes cluster


# How to run
### Local development
Run `sbt` on the root folder. Once you are in there:
- execute `r0` to run the first Akka node
Want another node?
Run `sbt` on the root folder. Once you are in there:
- execute `r1` to run the second Akka node
Want to reload the code?
 Press ENTER, UPARROW, ENTER on each of the opened consoles

### Is this production ready?
./minikube.sh

-------------------------------------
## Basics2
### sbt inside the root-project
- execute `clean` to apply the command to all *sub projects*
- execute `compile` to apply the command to all *sub projects*
- execute `test` to run all the tests in all the *sub projects*
- execute `testc` to run the test coverage report in all the *sub projects*

### sbt inside a sub-project
- execute `clean` to clean the target directory in the current *project*
- execute `compile` to compile in the current *project*
- execute `test` to run all the tests in the current *project*
- execute `testOnly *<spec_name>` to run a single spec in the current *project*
- execute `testc` to run the test coverage report process in the current *project*

### how to disable coverage in source code
```scala
// $COVERAGE-OFF$Reason for disabling coverage
// ...
// $COVERAGE-ON$
```