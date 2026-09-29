FROM apache/superset:5.0.0

USER root
RUN uv pip install --python /app/.venv/bin/python --no-cache psycopg2-binary==2.9.10
COPY --chown=superset:superset *.py /app/local/
USER superset
