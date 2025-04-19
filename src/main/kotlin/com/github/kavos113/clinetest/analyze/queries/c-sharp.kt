package com.github.kavos113.clinetest.analyze.queries

const val C_SHARP_QUERY = """
(class_declaration
 name: (identifier) @name.definition.class
) @definition.class

(interface_declaration
 name: (identifier) @name.definition.interface
) @definition.interface

(method_declaration
 name: (identifier) @name.definition.method
) @definition.method

(namespace_declaration
 name: (identifier) @name.definition.module
) @definition.module
"""