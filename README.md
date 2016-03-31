# jaws-stress-gaitling
Example of stress testing JAWS with gaitling.io using a TCPDS/TCPH populated database. Tested scenario executes
a series of queries, each query is placed into a file. For each query, the scenario waits via a log request until
the query ends on Spark/Hive and then fetches the results.
The results of such a simulation is added to the project in test directory.

