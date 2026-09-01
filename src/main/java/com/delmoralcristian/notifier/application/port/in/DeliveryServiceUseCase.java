package com.delmoralcristian.notifier.application.port.in;

import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;

public interface DeliveryServiceUseCase {

    void send(EventDTO eventDTO);

    void reSend(String eventId);
}
