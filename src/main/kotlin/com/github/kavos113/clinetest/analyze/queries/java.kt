package com.github.kavos113.clinetest.analyze.queries

const val JAVA_QUERY = """
(class_declaration
  name: (identifier) @name.definition.class) @definition.class

(method_declaration
  name: (identifier) @name.definition.method) @definition.method

(interface_declaration
  name: (identifier) @name.definition.interface) @definition.interface

(superclass (type_identifier) @name.reference.class) @reference.class
"""