-- Ejecutar en la base tuotu antes de desplegar la versión con expiración de sesiones.
-- Sesiones antiguas sin fecha se invalidan al aplicar esta migración.
ALTER TABLE sesion ADD COLUMN IF NOT EXISTS expira_en TIMESTAMP;
UPDATE sesion SET expira_en = CURRENT_TIMESTAMP WHERE expira_en IS NULL;
ALTER TABLE sesion ALTER COLUMN expira_en SET NOT NULL;
