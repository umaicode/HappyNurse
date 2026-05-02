-- 테스트용 초기 데이터 (로그인 테스트용)
-- password123! → BCrypt 해시

-- Organization
INSERT INTO organization (identifier_value, name, type_code, active, created_at, updated_at)
SELECT 'BIZ-001', '테스트병원', 'hospital', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE identifier_value = 'BIZ-001');

-- Ward
INSERT INTO ward (organization_id, ward_name)
SELECT o.organization_id, '내과병동'
FROM organization o
WHERE o.identifier_value = 'BIZ-001'
  AND NOT EXISTS (SELECT 1 FROM ward w WHERE w.ward_name = '내과병동' AND w.organization_id = o.organization_id);

-- Practitioner (nurse)
INSERT INTO practitioner (employee_number, password_hash, name, phone, created_at, updated_at)
SELECT 'EMP-001', '$2a$10$rgqTDj.xkGwqBdPcHvxtBu07Q2rrqstP296egpULznPDPareOeEYW', '김간호', '010-1234-5678', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM practitioner WHERE employee_number = 'EMP-001');

-- PractitionerRole (nurse → 내과병동)
INSERT INTO practitioner_role (practitioner_id, ward_id, role_code, period_start, created_at)
SELECT p.practitioner_id, w.ward_id, 'nurse', CURRENT_DATE, NOW()
FROM practitioner p, ward w
WHERE p.employee_number = 'EMP-001'
  AND w.ward_name = '내과병동'
  AND NOT EXISTS (
    SELECT 1 FROM practitioner_role pr
    WHERE pr.practitioner_id = p.practitioner_id AND pr.ward_id = w.ward_id AND pr.period_end IS NULL
  );