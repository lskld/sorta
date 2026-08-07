# Sorta

A mobile retail sales intelligence app with semantic vector search. 
Query sales and inventory data in plain language.

## Tech Stack

- **Client:** React Native (Expo)
- **Backend:** Kotlin + Ktor
- **Database:** PostgreSQL 18 + `pgvector` + `pg_trgm`
- **Embeddings:** local inference, ONNX Runtime + `bge-small-en-v1.5`

## Repo Structure

```
sorta/
├── backend/        Kotlin/Ktor API
├── client/         React Native (Expo) app
├── data/           dataset sourcing, cleaning, and category derivation scripts
└── README.md
```

## Setup

Follow these in order — later steps depend on earlier ones.

### Prerequisites

- PostgreSQL 18
- JDK 21 (Microsoft OpenJDK recommended)
- IntelliJ IDEA
- Python 3.9+ (for one-time model export + data prep scripts)
- Node.js + Expo CLI (for the client)

### 1. Database

Install PostgreSQL 18, then create the project database and enable both
extensions on it (extensions are per-database, not server-wide):

```sql
CREATE DATABASE sorta;
\c sorta
CREATE EXTENSION vector;
CREATE EXTENSION pg_trgm;
```

**Windows note:** pgvector has no prebuilt installer. Build from source using
Visual Studio (Desktop development with C++ workload) + `nmake`, run from the
"x64 Native Tools Command Prompt for VS":

```
set "PGROOT=C:\Program Files\PostgreSQL\18"
git clone --branch v0.8.5 https://github.com/pgvector/pgvector.git
cd pgvector
nmake /F Makefile.win
nmake /F Makefile.win install
```

If the build fails with `Cannot open include file: 'postgres.h'`, fall back
to a precompiled community build (e.g.
`github.com/andreiramani/pgvector_pgsql_windows`).

### 2. Backend (Kotlin/Ktor)

Open `backend/` in IntelliJ (open this folder specifically, not the repo
root — opening the monorepo root instead of `backend/` causes IntelliJ to
misresolve Gradle's working directory).

- **JDK:** 21 
- **Build system:** Gradle (Kotlin DSL). 
- **Engine:** Netty.

Dependencies (`build.gradle.kts`):

```kotlin
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation("com.microsoft.onnxruntime:onnxruntime:1.27.0")
    implementation("ai.djl.huggingface:tokenizers:0.36.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.pgvector:pgvector:0.1.6")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
```

Plugins block also needs the Kotlin serialization plugin:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}
```

...with a matching entry in `gradle/libs.versions.toml`:

```toml
[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Run the server (`Main.kt`) to confirm the hello-world route responds at
`http://localhost:8080`.

### 3. Embedding model (bge-small-en-v1.5 → ONNX)

One-time local export, run in Python, outside the Kotlin project:

```
pip install optimum[onnxruntime] transformers
cd backend/models
optimum-cli export onnx --model BAAI/bge-small-en-v1.5 --task feature-extraction ./bge-small-en-v1.5-onnx/
```

If `optimum-cli` isn't recognized (PATH issue on Windows), use the module
form instead:

```
python -m optimum.commands.optimum_cli export onnx --model BAAI/bge-small-en-v1.5 --task feature-extraction ./bge-small-en-v1.5-onnx/
```

Output lands in `backend/models/bge-small-en-v1.5-onnx/` (`model.onnx`,
`tokenizer.json`, `vocab.txt`, `config.json`, etc.). **This folder is
gitignored** — the ONNX file is ~130MB, over GitHub's 100MB limit. Re-run the
export command above to regenerate it on a fresh clone.

**Pooling note:** this model requires **CLS-token pooling** (index 0 of
`last_hidden_state`), followed by L2 normalization — not mean pooling. See
`backend/src/main/kotlin/embedding/Embedder.kt` for the reference
implementation.

### 4. Dataset — Online Retail II

Real UK online retailer transactions, 01/12/2009–09/12/2011, CC BY 4.0
licensed. Not committed to git — download manually:

- Canonical source: https://archive.ics.uci.edu/dataset/502/online+retail+ii
- Kaggle mirror (CSV): https://www.kaggle.com/datasets/mashlyn/online-retail-ii-uci

Place the downloaded file at:

```
data/raw/online_retail_II.csv
```

### 5. Schema

```
psql -U postgres -d sorta -f data/schema.sql
```

Creates `products` (with a `vector(384)` embedding column) and `sales`
(foreign-keyed to `products`). No `ivfflat`/`hnsw` index on the vector
column, no `pg_trgm` GIN index on `products.name` — deliberate, flat-scan
is fine at this row count (~1070 products).

### 6. Seed the database

Products first (sales has a foreign key to it):

```sql
\copy products (product_id, name, category) FROM 'data/seed/products.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

Sales needs a staging table, since `sales.csv` carries a `Description`
column the `sales` table intentionally doesn't store (already in
`products`, joined via `product_id`):

```sql
CREATE TEMP TABLE sales_staging (
    invoice_no TEXT, product_id TEXT, description TEXT, quantity INTEGER,
    sale_date TIMESTAMP, unit_price NUMERIC(10,2), customer_id TEXT, country TEXT
);
\copy sales_staging FROM 'data/seed/sales.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
INSERT INTO sales (product_id, invoice_no, quantity, unit_price, sale_date, customer_id, country)
SELECT product_id, invoice_no, quantity, unit_price, sale_date, customer_id, country FROM sales_staging;
DROP TABLE sales_staging;
```

Verify: `SELECT count(*) FROM products;` → 1070. `SELECT count(*) FROM sales;`
→ 2000. `SELECT max(sale_date) FROM sales;` → should match the reference
date step 6 printed.

### 7. Populate embeddings

`embedding` is `NULL` on every product until this runs. From `backend/`,
run `PopulateEmbeddings.kt` (`backend/src/main/kotlin/db/PopulateEmbeddings.kt`).
Loads the ONNX model once, embeds all 1070 products' names, writes each
vector back via `pgvector-java`.

Verify: `SELECT count(*) FROM products WHERE embedding IS NOT NULL;` → 1070.

**Before running any Kotlin file that connects to Postgres** (this one,
and the server itself): create a `.env` file at the repo root (gitignored,
not committed) with your local Postgres connection details:
 
```dotenv
DB_PORT=5432
DB_USER=user
DB_PASSWORD=password
```
 
Adjust the values to match your own local Postgres setup — port, username,
and password.

### 8. Client (React Native / Expo)

`client/` is a working Expo app: search screen, text input, results list,
tap-to-select anchor mechanism (selected row gets a highlighted ring, a
"Comparing to: [name] ×" chip appears, tap again or hit × to deselect).

Run with Expo Go for local development.

**Connecting to the backend:** the server runs at `http://localhost:8080`
on your dev machine, but `localhost` means something different depending
on how you're running the client:
- **iOS Simulator:** `localhost` works as-is.
- **Android Emulator:** use `10.0.2.2` instead of `localhost`.
- **Physical device via Expo Go:** use your dev machine's LAN IP address
  (e.g. `192.168.x.x`), not `localhost` — the phone is a separate device
  on the network, not the same machine.

## Running the full app

1. Start the backend server in IntelliJ (run `Main.kt` — this starts the
   Ktor server, loads the ONNX model, connects to Postgres, and computes
   the reference date, all at startup).
2. Confirm `http://localhost:8080` responds.
3. Start the Expo client (`npx expo start` from `client/`), open in Expo Go.
4. Search — type free text, optionally tap a result to set it as an
   anchor, search again to see anchor-constrained results.

## API Reference

**`POST /query`**

Request:
```json
{ "query_text": "string", "anchor_id": "string | null" }
```

Response:
```json
{
  "results": [
    { "product_id": "string", "name": "string", "category": "string",
      "distance": 0.0, "units_sold": 0 }
  ],
  "anchor_resolved": true
}
```

`anchor_id` must be a real `product_id` obtained from a previous
`/query` response's results — it is never derived from free text on the
server. `distance` is relative ranking, not an absolute similarity score.
`units_sold` is display-only; it does not affect ranking.