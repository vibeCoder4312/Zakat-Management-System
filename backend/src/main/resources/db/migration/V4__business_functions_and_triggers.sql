-- V4__business_functions_and_triggers.sql: Functions for business calculations
CREATE OR REPLACE FUNCTION fn_recalc_assessment(p_assessment_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_total_alloc NUMERIC(18,2);
    v_zakat_amt   NUMERIC(18,2);
    v_carry_fwd   NUMERIC(18,2);
    v_net_pay     NUMERIC(18,2);
    v_remaining   NUMERIC(18,2);
BEGIN
    SELECT zakat_amount, carry_forward
      INTO v_zakat_amt, v_carry_fwd
      FROM zk_assessment
     WHERE assessment_id = p_assessment_id;

    IF NOT FOUND THEN
        RETURN;
    END IF;

    -- Committed amount for ZAKAT fund type allocations:
    -- CASE WHEN status = 'PAID' THEN paid_amount
    --      WHEN status = 'PLANNED' THEN planned_amount
    --      ELSE 0 END
    SELECT COALESCE(SUM(
               CASE WHEN a.status = 'PAID' THEN a.paid_amount
                    WHEN a.status = 'PLANNED' THEN a.planned_amount
                    ELSE 0 END
           ), 0)
      INTO v_total_alloc
      FROM zk_allocation a
      JOIN zk_classification c ON c.classification_id = a.classification_id
     WHERE a.assessment_id = p_assessment_id
       AND c.fund_type = 'ZAKAT'
       AND a.status <> 'CANCELLED';

    v_net_pay   := v_zakat_amt - v_carry_fwd;
    v_remaining := v_net_pay - v_total_alloc;

    UPDATE zk_assessment
       SET net_payable            = v_net_pay,
           total_allocated_amount = v_total_alloc,
           remaining_balance      = v_remaining,
           updated_on             = CURRENT_TIMESTAMP
     WHERE assessment_id = p_assessment_id;
END;
$$ LANGUAGE plpgsql;
