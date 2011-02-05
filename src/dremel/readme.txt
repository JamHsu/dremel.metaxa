client          =>      The frontend to server module providing non-API access including 
                        command-line interface, REST, webapp and etc...


server          =>      API access to the functionality of the system. 
                        The facade and factory for the rest of modules.


common          =>      All common non-dremel-related library code. 


parser          =>      Parser to query language BQL and others...


compiler        =>      Query compiler of the parsed query text to executable query.


planner         =>      Tracks all storage resources and metadata, 
                        augments compiled query with storage locator and configuration. 
                        build physical query plan as executable object.


executor        =>      Executes physical query plan.


dataset         =>      The codec library for various object serialization formats 
                        including dremel nested columnar format.
