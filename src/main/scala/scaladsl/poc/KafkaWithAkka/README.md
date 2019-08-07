Each Transaction adds 2000 units.

Given 1000 Transactions, it is expected to reach a consistent state of 2000 * 1000, 2000000 units.

So.

We want a script that shoots 1000 transactions and stops.

And we want a script that performs GET of the api until the expected consistent state is reached.

Both return a timestamp at the end of execution.

Now we can ask the really important question of the test:

# For how long did the system stay unconsistent?

Run test.py to check it out for yourself!