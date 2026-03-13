import csv
import os
import random
import re
from datetime import datetime, date, timedelta
from decimal import Decimal

import psycopg2

# =========================
# Database configuration
# =========================
DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "adi_pharma",
    "user": "postgres",
    "password": "postgres",
}

# =========================
# CSV file paths
# =========================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

GENERIC_CSV = os.path.join(BASE_DIR, "generic.csv")
MANUFACTURER_CSV = os.path.join(BASE_DIR, "manufacturer.csv")
MEDICINE_CSV = os.path.join(BASE_DIR, "medicine.csv")


# =========================
# Helpers
# =========================
def safe_str(value):
    if value is None:
        return None
    value = str(value).strip()
    return value if value else None


def safe_int(value, default=0):
    if value is None:
        return default
    value = str(value).strip()
    if value == "":
        return default
    try:
        return int(float(value))
    except Exception:
        return default


def slugify_from_parts(code, name, row_id):
    text = f"{code}_{name}_{row_id}"
    text = text.strip()
    text = re.sub(r"\s+", "_", text)
    text = re.sub(r"[^\w_]", "", text)
    return text.lower()


def random_price():
    return Decimal(str(round(random.uniform(20, 500), 2)))


def random_qty():
    return random.randint(10, 200)


# =========================
# Insert generic
# =========================
def insert_generics(conn):
    print("Inserting adi_medicine_generic ...")

    inserted = 0
    skipped = 0
    counter = 1001

    with open(GENERIC_CSV, mode="r", encoding="utf-8-sig") as f, conn.cursor() as cur:
        reader = csv.DictReader(f)

        for row in reader:
            generic_name = safe_str(row.get("generic name"))
            if not generic_name:
                skipped += 1
                continue

            generic_code = f"G{counter}"
            counter += 1

            try:
                cur.execute(
                    """
                    INSERT INTO adi_medicine_generic (generic_code, generic_name, slug)
                    VALUES (%s, %s, NULL)
                    RETURNING id
                    """,
                    (generic_code, generic_name),
                )
                new_id = cur.fetchone()[0]

                slug = slugify_from_parts(generic_code, generic_name, new_id)

                cur.execute(
                    """
                    UPDATE adi_medicine_generic
                    SET slug = %s
                    WHERE id = %s
                    """,
                    (slug, new_id),
                )

                inserted += 1

            except psycopg2.Error as e:
                print(f"Skip generic [{generic_name}] بسبب error: {e}")
                conn.rollback()
                # reopen transaction
                with conn.cursor() as retry_cur:
                    pass

    conn.commit()
    print(f"Generic inserted: {inserted}, skipped: {skipped}")


# =========================
# Insert manufacturers
# =========================
def insert_manufacturers(conn):
    print("Inserting adi_medicine_manufacturals ...")

    inserted = 0
    skipped = 0
    counter = 1001

    with open(MANUFACTURER_CSV, mode="r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)

        for row in reader:
            manufacturer_name = safe_str(row.get("manufacturer name"))
            generics_count = safe_int(row.get("generics count"))
            brand_names_count = safe_int(row.get("brand names count"))

            if not manufacturer_name:
                skipped += 1
                continue

            manufacturer_code = f"F{counter}"
            counter += 1

            try:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO adi_medicine_manufacturals
                        (brand_names_count, generics_count, manufacturer_code, manufacturer_name, slug)
                        VALUES (%s, %s, %s, %s, NULL)
                        RETURNING id
                        """,
                        (
                            brand_names_count,
                            generics_count,
                            manufacturer_code,
                            manufacturer_name,
                        ),
                    )
                    new_id = cur.fetchone()[0]

                    slug = slugify_from_parts(manufacturer_code, manufacturer_name, new_id)

                    cur.execute(
                        """
                        UPDATE adi_medicine_manufacturals
                        SET slug = %s
                        WHERE id = %s
                        """,
                        (slug, new_id),
                    )

                conn.commit()
                inserted += 1

            except psycopg2.Error as e:
                conn.rollback()
                print(f"Skip manufacturer [{manufacturer_name}] because error: {e}")

    print(f"Manufacturer inserted: {inserted}, skipped: {skipped}")


# =========================
# Load lookup maps
# =========================
def load_generic_map(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT id, generic_name FROM adi_medicine_generic")
        rows = cur.fetchall()
    return {
        str(name).strip().lower(): row_id
        for row_id, name in rows
        if name is not None
    }


def load_manufacturer_map(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT id, manufacturer_name FROM adi_medicine_manufacturals")
        rows = cur.fetchall()
    return {
        str(name).strip().lower(): row_id
        for row_id, name in rows
        if name is not None
    }


# =========================
# Insert medicine details
# =========================
def insert_medicines(conn):
    print("Inserting adi_medicine_details ...")

    generic_map = load_generic_map(conn)
    manufacturer_map = load_manufacturer_map(conn)

    inserted = 0
    skipped = 0
    counter = 1001

    with open(MEDICINE_CSV, mode="r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)

        for row in reader:
            brand_name = safe_str(row.get("brand name"))
            dosage_form = safe_str(row.get("dosage form"))
            strength = safe_str(row.get("strength"))
            med_type = safe_str(row.get("type"))
            generic_name = safe_str(row.get("generic"))
            manufacturer_name = safe_str(row.get("manufacturer"))

            if not brand_name:
                skipped += 1
                continue

            generic_id = None
            manufacturer_id = None

            if generic_name:
                generic_id = generic_map.get(generic_name.lower())

            if manufacturer_name:
                manufacturer_id = manufacturer_map.get(manufacturer_name.lower())

            brand_code = f"M{counter}"
            counter += 1

            try:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO adi_medicine_details
                        (brand_code, brand_name, dosage_form, slug, strength, type, generic_id, manufacturer_id)
                        VALUES (%s, %s, %s, NULL, %s, %s, %s, %s)
                        RETURNING id
                        """,
                        (
                            brand_code,
                            brand_name,
                            dosage_form,
                            strength,
                            med_type,
                            generic_id,
                            manufacturer_id,
                        ),
                    )
                    new_id = cur.fetchone()[0]

                    slug = slugify_from_parts(brand_code, brand_name, new_id)

                    cur.execute(
                        """
                        UPDATE adi_medicine_details
                        SET slug = %s
                        WHERE id = %s
                        """,
                        (slug, new_id),
                    )

                conn.commit()
                inserted += 1

            except psycopg2.Error as e:
                conn.rollback()
                print(f"Skip medicine [{brand_name}] because error: {e}")

    print(f"Medicine inserted: {inserted}, skipped: {skipped}")


# =========================
# Insert stock price mapping
# =========================
def insert_stock_price_mapping(conn):
    print("Inserting adi_medicine_stock_price_mapping ...")

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM adi_medicine_details ORDER BY id")
        medicine_ids = [row[0] for row in cur.fetchall()]

    now = datetime.now()
    expire_date = date.today() + timedelta(days=100)

    inserted = 0

    for medicine_id in medicine_ids:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO adi_medicine_stock_price_mapping
                    (add_date, added_by, expire_date, price, qty, medicine_id)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                    (
                        now,
                        "admin",
                        expire_date,
                        random_price(),
                        random_qty(),
                        medicine_id,
                    ),
                )
            conn.commit()
            inserted += 1

        except psycopg2.Error as e:
            conn.rollback()
            print(f"Skip stock mapping for medicine_id [{medicine_id}] because error: {e}")

    print(f"Stock mapping inserted: {inserted}")


# =========================
# Main
# =========================
def main():
    print("Script directory:", BASE_DIR)
    print("Generic CSV:", GENERIC_CSV)
    print("Manufacturer CSV:", MANUFACTURER_CSV)
    print("Medicine CSV:", MEDICINE_CSV)

    for file_path in [GENERIC_CSV, MANUFACTURER_CSV, MEDICINE_CSV]:
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

    conn = None
    try:
        conn = psycopg2.connect(**DB_CONFIG)

        insert_generics(conn)
        insert_manufacturers(conn)
        insert_medicines(conn)
        insert_stock_price_mapping(conn)

        print("All data inserted successfully.")

    except Exception as e:
        if conn:
            conn.rollback()
        print("Error:", e)

    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()