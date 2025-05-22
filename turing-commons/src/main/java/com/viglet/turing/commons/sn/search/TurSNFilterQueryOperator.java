package com.viglet.turing.commons.sn.search;

/**
 * @author Alexandre Oliveira
 * @since 0.3.5
 */

public enum TurSNFilterQueryOperator{
    AND {
        @Override
        public String toString() {
            return "AND";
        }
    },
    OR {
        @Override
        public String toString() {
            return "OR";
        }
    },
    NONE {
        @Override
        public String toString() {
            return "NONE";
        }
    }
}
