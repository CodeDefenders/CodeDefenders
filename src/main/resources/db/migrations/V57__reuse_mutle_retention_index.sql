-- V53 already indexes (user_id, created_at); preserve applied migrations and remove the duplicate from V56.
DROP INDEX idx_mutle_guest_retention ON mutle_game;
