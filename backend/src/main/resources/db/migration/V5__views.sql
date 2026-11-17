-- V5__views.sql: Reconstructed reporting views

-- Detailed Allocation View
CREATE OR REPLACE VIEW zk_v_allocation AS
SELECT
    a.allocation_id,
    a.assessment_id,
    a.zakat_year,
    a.beneficiary_id,
    b.name AS beneficiary,
    b.beneficiary_category,
    a.classification_id,
    c.name AS classification,
    c.fund_type,
    a.category,
    a.status,
    a.approval_status,
    a.planned_amount,
    a.paid_amount,
    CASE 
        WHEN a.status = 'PAID' THEN a.paid_amount
        WHEN a.status = 'PLANNED' THEN a.planned_amount
        ELSE 0 
    END AS committed_amount,
    GREATEST(a.planned_amount - a.paid_amount, 0) AS outstanding_amount,
    a.paid_on,
    a.payment_method,
    sa.name AS paid_source_account,
    sa.region AS source_region,
    a.receipt_received,
    sc.name AS school,
    a.currency_code,
    a.foreign_amount,
    a.fx_rate,
    a.reference_no,
    a.current_std,
    a.prev_std_result,
    a.paid_via_agent,
    a.paid_via_employee,
    a.remarks,
    a.created_by,
    a.created_on,
    a.updated_by,
    a.updated_on
FROM zk_allocation a
JOIN zk_assessment asmnt ON asmnt.assessment_id = a.assessment_id
JOIN zk_classification c ON c.classification_id = a.classification_id
JOIN zk_beneficiary b ON b.beneficiary_id = a.beneficiary_id
LEFT JOIN zk_school sc ON sc.school_id = a.school_id
LEFT JOIN zk_source_account sa ON sa.source_account_id = a.paid_source_account_id
LEFT JOIN zk_agent ag ON ag.agent_id = a.paid_via_agent_id;

-- Assessment Year Summary View
CREATE OR REPLACE VIEW zk_v_year_summary AS
SELECT
    asmnt.zakat_year,
    asmnt.status AS year_status,
    asmnt.zakat_amount,
    asmnt.carry_forward,
    asmnt.net_payable,
    COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PAID' THEN a.paid_amount END), 0) AS total_paid,
    COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PLANNED' THEN GREATEST(a.planned_amount - a.paid_amount, 0) END), 0) AS total_pending,
    asmnt.net_payable
      - COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PAID' THEN a.paid_amount END), 0)
      - COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PLANNED' THEN GREATEST(a.planned_amount - a.paid_amount, 0) END), 0) AS unallocated,
    COALESCE(SUM(CASE WHEN c.fund_type = 'SADAQAH' AND a.status = 'PAID' THEN a.paid_amount END), 0) AS sadaqah_paid,
    COUNT(CASE WHEN a.status = 'PAID' THEN 1 END) AS paid_count,
    COUNT(CASE WHEN a.status = 'PLANNED' THEN 1 END) AS planned_count,
    COUNT(CASE WHEN a.status = 'PAID' AND a.receipt_received = 'N' THEN 1 END) AS receipts_missing,
    CASE 
        WHEN asmnt.net_payable > 0 THEN 
            ROUND((COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PAID' THEN a.paid_amount END), 0) / asmnt.net_payable * 100)::numeric, 1)
        ELSE 0 
    END AS pct_paid,
    CASE 
        WHEN asmnt.net_payable > 0 THEN 
            ROUND((COALESCE(SUM(CASE WHEN c.fund_type = 'ZAKAT' AND a.status = 'PLANNED' THEN GREATEST(a.planned_amount - a.paid_amount, 0) END), 0) / asmnt.net_payable * 100)::numeric, 1)
        ELSE 0 
    END AS pct_pending
FROM zk_assessment asmnt
LEFT JOIN zk_allocation a ON a.assessment_id = asmnt.assessment_id AND a.status <> 'CANCELLED'
LEFT JOIN zk_classification c ON c.classification_id = a.classification_id
GROUP BY asmnt.assessment_id, asmnt.zakat_year, asmnt.status, asmnt.zakat_amount, asmnt.carry_forward, asmnt.net_payable;
