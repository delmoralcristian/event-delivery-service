package com.delmoralcristian.notifier.application.port.in;

import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;

public interface DeliveryServiceUseCase {

    void send(EventDTO eventDTO);

    void reSend(NotificationEventEntity eventDTO);

}
