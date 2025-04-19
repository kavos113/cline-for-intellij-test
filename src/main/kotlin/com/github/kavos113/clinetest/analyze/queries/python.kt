package com.github.kavos113.clinetest.analyze.queries

const val PYTHON_QUERY = """
(class_definition
  name: (identifier) @name.definition.class) @definition.class

(function_definition
  name: (identifier) @name.definition.function) @definition.function
"""