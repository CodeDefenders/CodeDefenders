INSERT INTO text_settings
SELECT 'en', settings.name, settings.STRING_VALUE
FROM settings
WHERE settings.name IN ('SITE_NOTICE', 'PRIVACY_NOTICE', 'CONTACT_NOTICE');
