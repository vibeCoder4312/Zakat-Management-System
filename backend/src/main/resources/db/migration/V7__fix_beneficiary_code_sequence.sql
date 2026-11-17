-- V7__fix_beneficiary_code_sequence.sql
-- V3 set zk_beneficiary_code_seq to 969 while the last imported code was BEN-0968, so the first new
-- beneficiary skipped a number (BEN-0970). Align the sequence with the highest code actually in use.
-- Safe to run on a database that already has new beneficiaries: it never moves the sequence backwards
-- past an existing code.
SELECT setval(
    'zk_beneficiary_code_seq',
    GREATEST(
        (SELECT COALESCE(MAX(substring(beneficiary_code FROM 5)::int), 0)
           FROM zk_beneficiary
          WHERE beneficiary_code ~ '^BEN-[0-9]+$'),
        1
    )
);
