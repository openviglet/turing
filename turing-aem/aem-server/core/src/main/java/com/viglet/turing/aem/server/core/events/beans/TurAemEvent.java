package com.viglet.turing.aem.server.core.events.beans;

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
