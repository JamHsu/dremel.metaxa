executor
========

executes physical query execution plan on specified datasets, loading and buffering these 
datasets first.

In fact initially it is not really needed. Since the execution is synchronous and single-threaded
so just iterating the result iterator actually executes the physical query plan.


module ownership
================

1st February assigned to dgisrael@gmail.com to create from scratch   