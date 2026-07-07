-- Corrige nombres de iconos de categorías que no existen en Material Symbols Outlined.
-- Los nombres inválidos hacen que el icono se muestre como texto plano en el frontend.

UPDATE categories SET icon = 'content_cut'   WHERE name = 'Corte y Peinado';
UPDATE categories SET icon = 'auto_awesome'  WHERE name = 'Unas';
UPDATE categories SET icon = 'local_florist' WHERE name = 'Estetica Facial';
UPDATE categories SET icon = 'wb_sunny'      WHERE name = 'Depilacion';
UPDATE categories SET icon = 'spa'           WHERE name = 'Masajes y Bienestar';
UPDATE categories SET icon = 'water_drop'    WHERE name = 'Tratamientos Capilares';
UPDATE categories SET icon = 'shopping_bag'  WHERE name = 'Retail Profesional';
