CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE app_users (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    name varchar(160) NOT NULL, email varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL, role varchar(20) NOT NULL CHECK (role IN ('ADMIN','RECEPCAO')),
    active boolean NOT NULL DEFAULT true, auth_version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX app_users_email_ci ON app_users(lower(email));
CREATE TABLE patients (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    name varchar(160) NOT NULL, birth_date date NOT NULL, phone varchar(32) NOT NULL,
    email varchar(254), guardian_name varchar(160), guardian_relationship varchar(80), guardian_phone varchar(32),
    active boolean NOT NULL DEFAULT true
);
CREATE INDEX patients_name_idx ON patients(lower(name));
CREATE INDEX patients_phone_idx ON patients(phone);
CREATE TABLE clinic_services (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    name varchar(160) NOT NULL, description varchar(4000),
    duration_minutes integer NOT NULL CHECK (duration_minutes BETWEEN 1 AND 480),
    active boolean NOT NULL DEFAULT true, published boolean NOT NULL DEFAULT false
);
CREATE TABLE professionals (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    name varchar(160) NOT NULL, email varchar(254), phone varchar(32),
    registration varchar(80), region varchar(80), bio varchar(4000),
    active boolean NOT NULL DEFAULT true, published boolean NOT NULL DEFAULT false,
    CHECK (NOT published OR (nullif(trim(registration),'') IS NOT NULL AND nullif(trim(region),'') IS NOT NULL))
);
CREATE TABLE professional_services (
    professional_id uuid NOT NULL REFERENCES professionals(id),
    service_id uuid NOT NULL REFERENCES clinic_services(id),
    PRIMARY KEY (professional_id, service_id)
);
CREATE TABLE availability_periods (
    id uuid PRIMARY KEY, professional_id uuid NOT NULL REFERENCES professionals(id),
    day_of_week integer NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time time NOT NULL, end_time time NOT NULL, CHECK (start_time < end_time)
);
CREATE INDEX availability_prof_idx ON availability_periods(professional_id, day_of_week);
CREATE TABLE availability_blocks (
    id uuid PRIMARY KEY, professional_id uuid NOT NULL REFERENCES professionals(id),
    starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL, CHECK (starts_at < ends_at),
    EXCLUDE USING gist (professional_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)
);
CREATE TABLE appointments (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    patient_id uuid NOT NULL REFERENCES patients(id), professional_id uuid NOT NULL REFERENCES professionals(id),
    service_id uuid NOT NULL REFERENCES clinic_services(id), created_by uuid NOT NULL REFERENCES app_users(id),
    starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL,
    duration_minutes integer NOT NULL CHECK (duration_minutes BETWEEN 1 AND 480),
    status varchar(30) NOT NULL CHECK (status IN ('AGENDADO','CONFIRMADO','CONCLUIDO','CANCELADO','NAO_COMPARECEU')),
    CHECK (starts_at < ends_at),
    CONSTRAINT appointment_professional_overlap EXCLUDE USING gist
        (professional_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&) WHERE (status <> 'CANCELADO'),
    CONSTRAINT appointment_patient_overlap EXCLUDE USING gist
        (patient_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&) WHERE (status <> 'CANCELADO')
);
CREATE INDEX appointments_start_idx ON appointments(starts_at);
CREATE INDEX appointments_patient_idx ON appointments(patient_id, starts_at);
CREATE INDEX appointments_service_idx ON appointments(service_id);
CREATE TABLE appointment_history (
    id uuid PRIMARY KEY, appointment_id uuid NOT NULL REFERENCES appointments(id),
    actor_id uuid NOT NULL REFERENCES app_users(id), action varchar(40) NOT NULL,
    created_at timestamptz NOT NULL, previous_start timestamptz, new_start timestamptz,
    previous_status varchar(30), new_status varchar(30)
);
CREATE INDEX history_appointment_idx ON appointment_history(appointment_id, created_at);
CREATE TABLE audit_events (
    id uuid PRIMARY KEY, actor_id uuid REFERENCES app_users(id), action varchar(40) NOT NULL,
    entity_type varchar(60) NOT NULL, entity_id uuid NOT NULL, created_at timestamptz NOT NULL
);
CREATE INDEX audit_entity_idx ON audit_events(entity_type, entity_id, created_at);
CREATE TABLE clinic_settings (
    id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    display_name varchar(160) NOT NULL, description varchar(4000) NOT NULL,
    phone varchar(32) NOT NULL, email varchar(254) NOT NULL, whatsapp varchar(32) NOT NULL,
    public_address varchar(500), address_confirmed boolean NOT NULL DEFAULT false
);
INSERT INTO clinic_settings (id, created_at, updated_at, display_name, description, phone, email, whatsapp)
VALUES ('00000000-0000-0000-0000-000000000001', now(), now(), 'Espaço Sinapse',
    'Conheça o Espaço Sinapse e entre em contato para informações sobre atendimento em fonoaudiologia.',
    '(61) 98119-5462', 'fonoaudiologatati@gmail.com', '5561981195462');
