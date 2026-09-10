-- gen_random_uuid() requiert l'extension pgcrypto sur PostgreSQL < 13.
-- Sans effet (IF NOT EXISTS) sur les versions où gen_random_uuid() est natif (>= 13 avec pgcrypto)
-- ou pgcrypto est déjà présent.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
