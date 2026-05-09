# Arithmetic Service Specifications

The service exposes arithmetic operations on 32-bit signed integers (`int`).

## Operations

### add(a, b) → int
Returns the sum of `a` and `b`.
- Must return a SOAP Fault if the result overflows a 32-bit signed integer (outside `[-2147483648, 2147483647]`).

### subtract(a, b) → int
Returns `a` minus `b`.
- Must return a SOAP Fault if the result overflows.

### multiply(a, b) → int
Returns the product of `a` and `b`.
- Must return a SOAP Fault if the result overflows.

### divide(a, b) → int
Returns the integer quotient of `a` divided by `b` (truncated toward zero).
- Must return a SOAP Fault if `b` is zero.
