package com.runway.engagement.notifications.service;

import com.runway.engagement.notifications.dto.NotificationTemplateRequest;
import com.runway.engagement.notifications.exception.NotificationTemplateNotFoundException;
import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.model.NotificationTemplate;
import com.runway.engagement.notifications.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    public NotificationTemplate createTemplate(NotificationTemplate template) {
        return repository.save(template);
    }

    public NotificationTemplate getTemplateById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NotificationTemplateNotFoundException(
                                "Template not found with id: " + id
                        ));
    }

    public List<NotificationTemplate> getTemplates(String name, NotificationChannel channel) {
        if (name != null) {
            return List.of(
                    repository.findByName(name)
                            .orElseThrow(() ->
                                    new NotificationTemplateNotFoundException(
                                            "Template not found with name: " + name
                                    ))
            );
        }
        if (channel != null) {
            return repository.findByChannel(channel);
        }
        return repository.findAll();
    }

    public NotificationTemplate updateTemplate(Long id, NotificationTemplateRequest request) {
        NotificationTemplate existing = getTemplateById(id);
        existing.setName(request.getName());
        existing.setChannel(request.getChannel());
        existing.setSubject(request.getSubject());
        existing.setContent(request.getContent());
        return repository.save(existing);
    }

    public void deleteTemplate(Long id) {
        repository.delete(getTemplateById(id));
    }

    public Optional<NotificationTemplate> getTemplateByName(String templateName) {
        return repository.findByName(templateName);
    }

    public NotificationTemplate getTemplateByEvent(NotificationEvent event) {
        return repository.findByEvent(event)
                .orElseThrow(() ->
                        new IllegalArgumentException("No template for event: " + event));
    }
}