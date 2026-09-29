import os

SECRET_KEY = os.environ["SUPERSET_SECRET_KEY"]
if len(SECRET_KEY) < 32:
    raise ValueError("Run scripts\\Initialize-SupersetEnv.ps1 to set SUPERSET_SECRET_KEY.")

# Single-user local analytics; this volume is separate from simulator run storage.
SQLALCHEMY_DATABASE_URI = "sqlite:////app/superset_home/superset.db?check_same_thread=false"
SQLALCHEMY_TRACK_MODIFICATIONS = False
WTF_CSRF_ENABLED = True
SESSION_COOKIE_SAMESITE = "Lax"
SESSION_COOKIE_HTTPONLY = True
ENABLE_PROXY_FIX = False
# The analytics source is intentionally on Docker's private network.
PREVENT_UNSAFE_DB_CONNECTIONS = False
