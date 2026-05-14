-- ==============================================================================
-- SCRIPT DE MIGRACIÓN: Nexo -> Aeroxis (Supabase / PostgreSQL)
-- ==============================================================================
-- NOTA: Reemplaza "nexo_" con el prefijo real que tengan tus tablas actualmente.
-- Puedes ejecutar este script en el editor SQL de Supabase.

-- Ejemplo de renombrado de tablas (Descomenta y ajusta según tus tablas reales):
-- ALTER TABLE nexo_users RENAME TO aeroxis_users;
-- ALTER TABLE nexo_clans RENAME TO aeroxis_clans;
-- ALTER TABLE nexo_islands RENAME TO aeroxis_islands;
-- ALTER TABLE nexo_economy RENAME TO aeroxis_economy;

-- Si tienes secuencias (IDs autoincrementables) asociadas, opcionalmente puedes renombrarlas:
-- ALTER SEQUENCE nexo_users_id_seq RENAME TO aeroxis_users_id_seq;

-- Si tienes funciones en PostgreSQL que usaban la palabra nexo:
-- ALTER FUNCTION nexo_calculate_stats RENAME TO aeroxis_calculate_stats;

-- ==============================================================================
-- PRECAUCIÓN: Asegúrate de actualizar el código en Java (DatabaseManager)
-- para que las consultas (SELECT, INSERT, UPDATE) ahora apunten a "aeroxis_..."
-- ==============================================================================
