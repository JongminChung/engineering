import os

import pandas as pd
from sqlalchemy import create_engine


def main() -> None:
    dataset_url = os.environ.get("DATASET_URL")
    database_url = os.environ.get("DATABASE_URL")
    table_name = os.environ.get("TABLE_NAME", "superstore_sales")

    if not dataset_url or not database_url:
        raise SystemExit("DATASET_URL and DATABASE_URL must be set")

    df = pd.read_csv(dataset_url)

    engine = create_engine(database_url)
    with engine.begin() as connection:
        df.to_sql(table_name, connection, if_exists="replace", index=False)


if __name__ == "__main__":
    main()
