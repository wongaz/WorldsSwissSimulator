import os

import psycopg2
from psycopg2 import sql


def main():
    reader_password = os.environ["SUPERSET_READER_PASSWORD"]
    if not reader_password:
        raise ValueError("Run scripts\\Initialize-SupersetEnv.ps1 to set SUPERSET_READER_PASSWORD.")
    owner = os.environ["SIM_DB_USER"]
    with psycopg2.connect(
        host="postgres",
        dbname="worlds_swiss",
        user=owner,
        password=os.environ["SIM_DB_PASSWORD"],
        connect_timeout=10,
    ) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1 FROM pg_roles WHERE rolname = 'superset_reader'")
            if cursor.fetchone() is None:
                cursor.execute("CREATE ROLE superset_reader LOGIN")
            cursor.execute(
                "ALTER ROLE superset_reader WITH NOSUPERUSER NOCREATEDB NOCREATEROLE "
                "NOREPLICATION NOBYPASSRLS PASSWORD %s",
                (reader_password,),
            )
            cursor.execute("ALTER ROLE superset_reader SET default_transaction_read_only = on")
            cursor.execute("GRANT CONNECT ON DATABASE worlds_swiss TO superset_reader")
            cursor.execute("GRANT USAGE ON SCHEMA public TO superset_reader")
            cursor.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO superset_reader")
            cursor.execute(
                sql.SQL(
                    "ALTER DEFAULT PRIVILEGES FOR ROLE {} IN SCHEMA public "
                    "GRANT SELECT ON TABLES TO superset_reader"
                ).format(sql.Identifier(owner))
            )
    print("Read-only PostgreSQL access configured for Superset.")


if __name__ == "__main__":
    main()
