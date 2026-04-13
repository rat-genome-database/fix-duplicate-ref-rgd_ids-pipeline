# fix-duplicate-ref-rgd_ids-pipeline

Fixes the problem of duplicate PubMed IDs for references in RGD.

## Problem

A given PubMed ID should map to exactly one active reference RGD ID. This pipeline detects cases where a PubMed ID has been assigned more than one active reference RGD ID, and consolidates them into a single canonical record.

## Processing Flow

1. Query all PubMed IDs that have more than one active reference RGD ID in `RGD_ACC_XDB`.
2. For each such PubMed ID, identify the canonical (most recently created, or highest-numbered) RGD ID as the replacement.
3. For each duplicate (non-canonical) RGD ID:
   - **Remap annotations** — any annotation referencing the old RGD ID is updated to use the replacement, unless an identical annotation already exists under the replacement.
   - **Remap reference associations** — all object-to-reference associations are moved from the old RGD ID to the replacement.
   - **Withdraw** the old RGD ID.
   - **Record history** in `RGD_ID_HISTORY`.

## Logs

| File | Contents |
|------|----------|
| `logs/status.log` | Run summary |
| `logs/updates.log` | Details of all remapped annotations and associations |
