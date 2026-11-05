---
"@viglet/turing-sdk": patch
"@viglet/turing-react-sdk": patch
---

`fetchSearch` sends the `fqAnd`, `fqOr`, `fqOperator` and `fqItemOperator` its search params take, as `fq.and[]`, `fq.or[]`, `fq.op` and `fqi.op` (T1223). Before, the GET search dropped them, so `fqItemOperator: "OR"` still intersected two values of one field.
