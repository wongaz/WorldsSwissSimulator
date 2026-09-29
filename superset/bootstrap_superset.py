import os

from sqlalchemy import create_engine, inspect
from sqlalchemy.engine.url import URL
from superset import db, security_manager
from superset.app import create_app


def main():
    password = os.environ["SUPERSET_ADMIN_PASSWORD"]
    if not password:
        raise ValueError("Run scripts\\Initialize-SupersetEnv.ps1 to set SUPERSET_ADMIN_PASSWORD.")
    uri = URL.create(
        "postgresql+psycopg2",
        username="superset_reader",
        password=os.environ["SUPERSET_READER_PASSWORD"],
        host="postgres",
        port=5432,
        database="worlds_swiss",
    ).render_as_string(hide_password=False)

    app = create_app()
    with app.app_context():
        from superset.connectors.sqla.models import SqlaTable
        from superset.models.core import Database

        if security_manager.find_user(username="admin") is None:
            admin = security_manager.add_user(
                username="admin",
                first_name="Local",
                last_name="Administrator",
                email="admin@localhost.invalid",
                role=security_manager.find_role("Admin"),
                password=password,
            )
            if not admin:
                raise RuntimeError("Could not create the Superset administrator.")

        database = db.session.query(Database).filter_by(database_name="Worlds Swiss").one_or_none()
        if database is None:
            database = Database(database_name="Worlds Swiss")
            db.session.add(database)
        database.set_sqlalchemy_uri(uri)
        database.expose_in_sqllab = True
        database.allow_dml = False
        database.allow_ctas = False
        database.allow_cvas = False
        database.allow_file_upload = False
        db.session.commit()

        engine = create_engine(uri)
        try:
            tables = inspect(engine).get_table_names(schema="public")
            for name in ("simulation_runs", "team_run_results"):
                if name not in tables:
                    print(f"{name} is not present yet; start the simulator, then rerun superset-init.")
                    continue
                dataset = db.session.query(SqlaTable).filter_by(
                    database_id=database.id, schema="public", table_name=name
                ).one_or_none()
                if dataset is None:
                    dataset = SqlaTable(database=database, schema="public", table_name=name)
                    db.session.add(dataset)
                    db.session.flush()
                    dataset.fetch_metadata()
            db.session.commit()
        finally:
            engine.dispose()
    print("Superset is linked to Worlds Swiss; existing chart definitions are preserved.")


if __name__ == "__main__":
    main()
