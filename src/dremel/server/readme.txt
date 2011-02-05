Server module
=============

API access to the functionality of the system. 
The facade and factory pattern implementation for the almost all the rest of modules. 
Server module implements a facade pattern, meaning it just delegates the requests for processing 
to various helper modules in the right order and sends back the final results. 

Requests almost always is a query to run but also can be a request to convert/import/export 
data stored in one file to another. Server's requests processing looks like following: 

1) Server accepts the requests
2) Uses parser module to parse it
3) Then uses compiler module to analyze it and compile it into piece of executable code 
4) Uses planner to convert the compiled query into physical execution plan. 
5) Physical execution plan is just a compiled query with a list of the data locations 
and a bunch of configuration parameters that is passed to compiled query as arguments upon its 
execution. 
6) Then Server uses executor to execute or run the physical execution plan 


Initially only expressions will be dynamically generated into hava code and dynamically compiled 
to java bytecode. The rest of query will be supplied as static compile-time java classes. 
They are mostly iterators and loops constraints and are not very dependent on query, 
so to ease debugging we will not dynamically generate and compile these constructs. 
However in subsequent revisions of Metaxa, query can be turned into single piece of java code 
including the inner loops themselves as well as even all the code needed for nested query support. 
That single piece of java code will be compiled and run on executor. 
Server module is also functioning as factory to create correct implemenation for all modules. 
All the rest of modules use each other only via interfaces.

In future another interface will be provided asynchronousServer, 
which will provide functionality of enqueueing requests as well as functionality 
for supporting multiple users and maintaining sesssions and providing security/control features.



Module ownership
================

1st February: pconstantine@gmail.com is assigned to creeate from scratch.