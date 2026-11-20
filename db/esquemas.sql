-- Una sola base de datos, un esquema por microservicio.
-- Normalmente NO hace falta ejecutarlo: cada servicio crea su esquema al arrancar
-- (hibernate.hbm2ddl.create_namespaces=true). Úsalo solo si ves el error
-- "schema ... does not exist".
CREATE SCHEMA IF NOT EXISTS usuarios;
CREATE SCHEMA IF NOT EXISTS reservas;
CREATE SCHEMA IF NOT EXISTS notificaciones;
