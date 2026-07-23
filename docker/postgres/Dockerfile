FROM postgres:16-alpine
# Imagen ligera reutilizada tanto para primary como para réplicas;
# el rol se decide en runtime vía entrypoint-scripts + variables de entorno
# (POSTGRES_ROLE=primary|replica), sin necesidad de imágenes separadas.
COPY init-role.sh /docker-entrypoint-initdb.d/init-role.sh
RUN chmod +x /docker-entrypoint-initdb.d/init-role.sh
