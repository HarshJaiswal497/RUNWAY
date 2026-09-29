INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'TICKET_ASSIGNED', 'TICKET_ASSIGNED_INAPP', 'PUSH',
       'Advisor assigned — #{{ticketId}}',
       'Your case has been picked up. An advisor is now reviewing your request and will reply shortly.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'TICKET_ASSIGNED');

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'TICKET_REPLY', 'TICKET_REPLY_INAPP', 'PUSH',
       'New reply on your request — #{{ticketId}}',
       '{{preview}}'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'TICKET_REPLY');
