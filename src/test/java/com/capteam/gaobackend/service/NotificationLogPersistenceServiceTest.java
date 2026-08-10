package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationLogPersistenceServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Test
    void savesNotificationLogThroughRepository() {
        NotificationLog log = mock(NotificationLog.class);
        when(notificationLogRepository.save(log)).thenReturn(log);
        NotificationLogPersistenceService service =
                new NotificationLogPersistenceService(notificationLogRepository);

        assertThat(service.save(log)).isSameAs(log);
        verify(notificationLogRepository).save(log);
    }

    @Test
    void alwaysStartsNewTransactionWhenSavingLog() throws NoSuchMethodException {
        Method method = NotificationLogPersistenceService.class
                .getMethod("save", NotificationLog.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
