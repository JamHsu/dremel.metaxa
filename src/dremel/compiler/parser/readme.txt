parser 
=============

* parser module is responsible to parse query and other requests in predefined language.

* parser accepts an input string in text format and return matching abstract syntax tree.

* parser must report in proper way all syntax errors with friendly messages and location of error and explanation what is wrong in the syntax.




design
===========
basically classic ANTLR inspired design




TODO
=============

1. bql.g must be well documented inside. The fact that there are two sets of rules must be described in the bql.g file. As well links to antlr documentations and refernces to antlr books must be mentioned in the bql.g file. So if one ecamines it, she could find her way herself to the references without resorting to questioning.

2. As well astnode.java file must be well documented, since many code contributors end up looking there for clues how to work with parsed data. astnode.java file must contain a note to the reader that he must also look into bql.g file for more info.

3. current parser code must be checked on how it works when there are syntax errors and few unit tests written on that,

4. All related files must be brought here and interface created and used in unit tests showing proper usage of parser module. Creating a stub implementation can be skipped for this module because ANTLR implementation is very small and very fast, and already implemented and well tested.




module ownership
================

* form 1st February, hongsan@gmail.com
* before that created by camuel@gmail.com