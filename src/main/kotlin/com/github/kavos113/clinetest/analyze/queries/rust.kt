package com.github.kavos113.clinetest.analyze.queries

const val RUST_QUERY = """
(struct_item
    name: (type_identifier) @name.definition.class) @definition.class

(enum_item
    name: (type_identifier) @name.definition.class) @definition.class

(union_item
    name: (type_identifier) @name.definition.class) @definition.class

(type_item
    name: (type_identifier) @name.definition.class) @definition.class

(declaration_list
    (function_item
        name: (identifier) @name.definition.method)) @definition.method

(function_item
    name: (identifier) @name.definition.function) @definition.function

(trait_item
    name: (type_identifier) @name.definition.interface) @definition.interface

(mod_item
    name: (identifier) @name.definition.module) @definition.module

(macro_definition
    name: (identifier) @name.definition.macro) @definition.macro
"""