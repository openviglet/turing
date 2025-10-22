package com.viglet.turing.connector.aem.commons.bean;

public enum TurAemEvent {
    UNPUBLISHING {
        public String toString() {
            return "UNPUBLISHING";
        }
    },
    PUBLISHING {
        public String toString() {
            return "PUBLISHING";
        }
    },
    NONE {
        public String toString() {
            return "NONE";
        }
    }
}
