package com.viglet.turing.system.email.provider;

import com.viglet.turing.system.email.TurEmailMessage;

public interface TurEmailProvider {
    String getProviderType();

    void sendEmail(TurEmailMessage message, String apiKey);
}
