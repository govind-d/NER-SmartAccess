# Running on localhost without Docker

This is the setup that is actually running on this machine, verified end to end. Docker
Desktop is not used at all: its WSL2 virtual machine reserves 2-4 GB before PostgreSQL
even starts, and a native PostgreSQL service idles at roughly 200 MB.

---

## What is installed

| Component | Location | Notes |
|---|---|---|
| PostgreSQL 17.6 | `E:\workspace\pgsql17` | EDB "binaries only" zip, extracted - no installer, no admin rights |
| PostGIS 3.6.2 | merged into the same folder | `postgis-bundle-pg17-3.6.2x64` from download.osgeo.org |
| Data cluster | `E:\workspace\pgdata` | created with `initdb`, listening on 5432 |

**Why not the pre-installed PostgreSQL 18** at `C:\Program Files\PostgreSQL\18`: its
`initdb` crashes with an access violation (`0xC0000005`) during post-bootstrap
initialisation, both from the original installation and from a copy. That is why it was
installed but never initialised and has no registered service. PostgreSQL 17.6
initialises cleanly on the same machine.

**How PostGIS was merged:** the bundle ships its own OpenSSL and zlib DLLs, and letting
them overwrite PostgreSQL's own copies breaks the server. The merge is therefore additive
only - `robocopy /XC /XN /XO`, which copies just the files that do not already exist, so
GEOS, PROJ, GDAL and the extension SQL are added while PostgreSQL keeps its own runtime.

---

## Starting everything

**1. Database**

```bash
E:/workspace/pgsql17/bin/pg_ctl.exe -D E:/workspace/pgdata -l E:/workspace/pgdata/server.log -o "-p 5432" start
```

Check it:

```bash
E:/workspace/pgsql17/bin/pg_isready.exe -h localhost -p 5432
```

**2. Backend** (reads `backend/.env`, which already points at `localhost:5432`)

```bash
cd backend && ./run-dev.ps1
```

**3. Frontend**

```bash
cd frontend && npm run dev
```

> Port 5173 is occupied on this machine by an unrelated Vite server from the
> `govtId-validation` project, so Vite falls back to **5174** and `FRONTEND_ORIGIN` in
> `backend/.env` is set to `http://localhost:5174` to match. If you free 5173, change
> that value back or the STOMP handshake will be rejected.

---

## Stopping

```bash
E:/workspace/pgsql17/bin/pg_ctl.exe -D E:/workspace/pgdata stop
```

The backend and frontend are stopped with Ctrl+C in their terminals. Note that
`spring-boot:run` forks a separate JVM: killing Maven does not always kill it, and the
orphan keeps port 8080. If a restart fails with "Port 8080 was already in use", find and
stop that java process.

---

## Recreating the database from scratch

The cluster is disposable - everything is rebuilt by Flyway and the seeder.

```bash
E:/workspace/pgsql17/bin/psql.exe -h localhost -U postgres -c "DROP DATABASE nersmartlogix"
```

```bash
E:/workspace/pgsql17/bin/createdb.exe -h localhost -U postgres -O nerapp nersmartlogix
```

```bash
E:/workspace/pgsql17/bin/psql.exe -h localhost -U nerapp -d nersmartlogix -c "CREATE EXTENSION postgis"
```

Then start the backend: Flyway applies V1-V8 and the seeder recreates the demo data.

---

## Database credentials

`nerapp` is a **superuser** in this local cluster. That is deliberate and local-only:
Flyway's `V1__enable_postgis.sql` runs `CREATE EXTENSION postgis`, which requires
superuser rights. A real deployment would have a DBA create the extension once and run
the application as an ordinary owner.

The password is in `backend/.env` (git-ignored), generated randomly at setup time.
