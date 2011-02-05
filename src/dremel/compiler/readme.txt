Compiler
==========

Query compiler of the parsed query text to executable query.

Initially only expressions will be dynamically generated into java code and subsequently 
dynamically compiled to java bytecode. The rest of the query will stay as static compile-time java 
classes. They are mostly static inner loops and are not very dependent on query, 
so to ease debugging we will not dynamically generate and compile these constructs. 

However in future, query can be turned into single piece of java code including all the inner loops.
That single piece of java code will be compiled and run on executor probably as separate thread.


module ownership
================
1st February assigned to hongsan@gmail.com
   