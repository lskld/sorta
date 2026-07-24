import csv
import random
from collections import Counter, defaultdict
from datetime import datetime

from categorize_products import classify  # reuses the keyword rules we already validated

RAW_PATH = "raw/online_retail_II.csv"
SALES_OUT_PATH = "seed/sales.csv"
PRODUCTS_OUT_PATH = "seed/products.csv"

TARGET_SALES_ROWS = 2000
RANDOM_SEED = 42  # fixed, so the sample is reproducible across runs/demos

# Real product stock codes in this dataset are ~5 digits, optionally with a
# trailing letter (e.g. "85123A"). Non-product rows (postage, fees, manual
# adjustments, bank charges) use alphabetic codes instead - this pattern is
# a more reliable filter than blacklisting descriptions by hand.
import re
PRODUCT_CODE_PATTERN = re.compile(r"^\d{4,5}[A-Za-z]?$")


def parse_date(raw: str) -> datetime:
    # Mirrors of this dataset use slightly different date formats. Try the
    # common ones; raise clearly if none match so the real format can be
    # reported back rather than silently mis-parsing.
    for fmt in ("%m/%d/%Y %H:%M", "%Y-%m-%d %H:%M:%S", "%d/%m/%Y %H:%M"):
        try:
            return datetime.strptime(raw, fmt)
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {raw!r}")


def load_and_filter(path: str):
    kept = []
    dropped_examples = []

    with open(path, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            invoice = row.get("Invoice") or row.get("InvoiceNo") or ""
            stock_code = (row.get("StockCode") or "").strip()
            description = (row.get("Description") or "").strip()
            quantity_raw = row.get("Quantity") or "0"
            price_raw = row.get("Price") or row.get("UnitPrice") or "0"

            reason = None
            if invoice.upper().startswith("C"):
                reason = "cancellation (InvoiceNo starts with C)"
            elif not PRODUCT_CODE_PATTERN.match(stock_code):
                reason = f"non-product stock code ({stock_code!r})"
            elif not description:
                reason = "blank description"
            else:
                try:
                    if float(quantity_raw) <= 0:
                        reason = "non-positive quantity"
                    elif float(price_raw) <= 0:
                        reason = "non-positive price"
                except ValueError:
                    reason = "unparseable quantity/price"

            if reason:
                if len(dropped_examples) < 20:
                    dropped_examples.append((reason, description or stock_code))
                continue

            kept.append(row)

    return kept, dropped_examples


def pick_best_window(rows, window_months: int = 3):
    monthly_counts = Counter()
    for row in rows:
        date = parse_date(row["InvoiceDate"])
        monthly_counts[(date.year, date.month)] += 1

    months_sorted = sorted(monthly_counts.keys())
    print("\nMonthly transaction counts (post-filter):")
    for ym in months_sorted:
        print(f"  {ym[0]}-{ym[1]:02d}: {monthly_counts[ym]}")

    best_window = None
    best_total = -1
    for i in range(len(months_sorted) - window_months + 1):
        window = months_sorted[i:i + window_months]
        # only accept contiguous month sequences
        if window[-1][1] - window[0][1] + 12 * (window[-1][0] - window[0][0]) != window_months - 1:
            continue
        total = sum(monthly_counts[m] for m in window)
        if total > best_total:
            best_total = total
            best_window = window

    print(f"\nAuto-selected window: {best_window[0]} through {best_window[-1]} "
          f"({best_total} transactions available)")
    return set(best_window)


def main():
    print(f"Loading and filtering {RAW_PATH} ...")
    kept, dropped_examples = load_and_filter(RAW_PATH)
    print(f"Kept {len(kept)} rows after filtering.")
    print("\nSample of dropped rows (first 20):")
    for reason, label in dropped_examples:
        print(f"  [{reason}] {label}")

    window_months = pick_best_window(kept)
    windowed_rows = [
        row for row in kept
        if (parse_date(row["InvoiceDate"]).year, parse_date(row["InvoiceDate"]).month) in window_months
    ]
    print(f"\n{len(windowed_rows)} rows available inside the selected window.")

    random.seed(RANDOM_SEED)
    sample_size = min(TARGET_SALES_ROWS, len(windowed_rows))
    if sample_size < TARGET_SALES_ROWS:
        print(f"WARNING: only {sample_size} rows available in-window, "
              f"target was {TARGET_SALES_ROWS}.")
    sales_sample = random.sample(windowed_rows, sample_size)

    ref_date = max(parse_date(r["InvoiceDate"]) for r in sales_sample)
    print(f"\nReference date (MAX sale_date in sample): {ref_date}")

    # --- Write sales.csv ---
    with open(SALES_OUT_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "InvoiceNo", "StockCode", "Description", "Quantity",
            "InvoiceDate", "UnitPrice", "CustomerID", "Country"
        ])
        writer.writeheader()
        for row in sales_sample:
            writer.writerow({
                "InvoiceNo": row.get("Invoice") or row.get("InvoiceNo"),
                "StockCode": row["StockCode"],
                "Description": row["Description"],
                "Quantity": row["Quantity"],
                "InvoiceDate": row["InvoiceDate"],
                "UnitPrice": row.get("Price") or row.get("UnitPrice"),
                "CustomerID": row.get("Customer ID") or row.get("CustomerID"),
                "Country": row.get("Country"),
            })

    # --- Derive distinct products, pick canonical (most common) description
    #     per stock code, classify category ---
    stock_code_descriptions = defaultdict(Counter)
    for row in sales_sample:
        stock_code_descriptions[row["StockCode"]][row["Description"]] += 1

    with open(PRODUCTS_OUT_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["StockCode", "Description", "Category"])
        writer.writeheader()
        for stock_code, desc_counts in stock_code_descriptions.items():
            canonical_description = desc_counts.most_common(1)[0][0]
            matches = classify(canonical_description)
            category = matches[0] if matches else "Uncategorized"
            writer.writerow({
                "StockCode": stock_code,
                "Description": canonical_description,
                "Category": category,
            })

    print(f"\nWrote {len(sales_sample)} sales rows to {SALES_OUT_PATH}")
    print(f"Wrote {len(stock_code_descriptions)} distinct products to {PRODUCTS_OUT_PATH}")


if __name__ == "__main__":
    main()
