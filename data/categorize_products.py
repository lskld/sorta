import csv
import re
from collections import Counter

# Priority order matters: if a description matches multiple categories,
# whichever category appears first in this dict wins when we assign a
# single category later. Order below is a first guess, not validated yet -
# the multi-match sample at the bottom will tell us if it needs reordering.
CATEGORIES = {
    "Kitchen & Dining": ["MUG", "CUP", "BOWL", "TEA", "CAKE", "BAKING", "COOKIE", "JAR", "JUG",
                          "PLATE", "SAUCER", "CUTLERY", "APRON", "NAPKINS", "DOILIES", "TISSUES",
                          "PICNIC", "WARMER", "COFFEE", "MILK", "PANTRY", "KITCHEN", "LUNCH",
                          "BOTTLE", "WATER", "GLOVE", "GLOVES"],
    "Home Decor & Lighting": ["SIGN", "HOLDER", "HANGING", "LIGHT", "LIGHTS", "LAMP", "CANDLE",
                               "CANDLES", "DECORATION", "GARLAND", "BUNTING", "FRAME", "PICTURE",
                               "MIRROR", "CLOCK", "STAR", "CUSHION", "MAT", "DOORMAT", "DOOR",
                               "HOOK", "HANGER", "KNOB", "CABINET", "DRAWER", "STAND", "STORAGE",
                               "DOORSTOP"],
    "Bags & Storage": ["BAG", "CASES", "SHOPPER", "BOX", "BOXES", "TIN", "TINS", "TRINKET",
                        "BASKET", "WICKER"],
    "Stationery & Gift Wrap": ["CARD", "CARDS", "WRAP", "RIBBON", "RIBBONS", "GIFT", "PAPER",
                                "NOTEBOOK", "PENCILS", "PEN", "ALPHABET", "MAGNETS"],
    "Seasonal & Occasion": ["CHRISTMAS", "EASTER", "BIRTHDAY", "PARTY", "WREATH"],
    "Garden & Nature": ["GARDEN", "FLOWER", "BIRD", "BUTTERFLY", "DUCK", "DUCKS"],
    "Craft & Hobby": ["KIT", "SEWING", "CRAFT"],
    "Gift & Novelty": ["HEART", "HEARTS", "ROSE", "LOVE", "SWEETHEART", "FAIRY", "WOODLAND",
                        "CIRCUS", "PARADE", "MAGIC", "SKULL", "SKULLS", "TOY", "DOLL", "DOLLY",
                        "DINOSAUR", "DINOSAURS", "UMBRELLA", "PARASOL", "KEY", "MONEY", "TRAVEL",
                        "PHOTO", "ALARM", "DOG", "CAT", "RABBIT", "STRAWBERRY", "GLOBE", "MONSTER",
                        "BLOCK", "LETTERS"],
    "Kids": ["CHILDS", "CHILDRENS", "GIRL", "MOBILE", "GAME"],
}


def classify(description: str) -> list[str]:
    """Return every category whose keyword list matches this description
    (word-boundary match, case-insensitive). Empty list = uncategorized."""
    desc_upper = description.upper()
    matches = []
    for category, keywords in CATEGORIES.items():
        for kw in keywords:
            if re.search(rf"\b{kw}\b", desc_upper):
                matches.append(category)
                break  # found one keyword hit for this category, move on
    return matches

if __name__ == "__main__":
    # --- Load every distinct description, weighted by how many rows use it ---
    # (row-weight matters: a handful of very common products account for a
    # disproportionate share of actual transactions, so "% of rows covered"
    # is a more meaningful number than "% of distinct descriptions covered.")
    desc_row_counts = Counter()
    with open("online_retail_II.csv", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            desc = row.get("Description", "").strip()
            if desc:
                desc_row_counts[desc] += 1

    total_distinct = len(desc_row_counts)
    total_rows = sum(desc_row_counts.values())

    uncategorized = []
    multi_match = []
    covered_distinct = 0
    covered_rows = 0

    for desc, row_count in desc_row_counts.items():
        matches = classify(desc)
        if not matches:
            uncategorized.append(desc)
        else:
            covered_distinct += 1
            covered_rows += row_count
            if len(matches) > 1:
                multi_match.append((desc, matches))

    print(f"Total distinct descriptions: {total_distinct}")
    print(f"Total rows: {total_rows}")
    print()
    print(f"Covered (distinct descriptions): {covered_distinct} ({covered_distinct/total_distinct:.1%})")
    print(f"Uncovered (distinct descriptions): {len(uncategorized)} ({len(uncategorized)/total_distinct:.1%})")
    print()
    print(f"Covered (rows): {covered_rows} ({covered_rows/total_rows:.1%})")
    print(f"Uncovered (rows): {total_rows - covered_rows} ({(total_rows - covered_rows)/total_rows:.1%})")
    print()
    print(f"Descriptions matching MULTIPLE categories: {len(multi_match)} ({len(multi_match)/total_distinct:.1%})")
    print()
    print("Sample of 30 UNCATEGORIZED descriptions:")
    for desc in uncategorized[:30]:
        print(" -", desc)
    print()
    print("Sample of 15 MULTI-MATCH descriptions:")
    for desc, cats in multi_match[:15]:
        print(" -", desc, "->", cats)