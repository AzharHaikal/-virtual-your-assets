-- Profile preferences kept on the server so every device of the member shows the same (Phase 1.10).
ALTER TABLE idp.member
    ADD COLUMN theme    varchar(8) NOT NULL DEFAULT 'LIGHT',
    ADD COLUMN language varchar(2) NOT NULL DEFAULT 'EN',
    ADD COLUMN currency varchar(3) NOT NULL DEFAULT 'IDR',   -- display only, no exchange rates
    ADD CONSTRAINT member_theme_check CHECK (theme IN ('LIGHT', 'DARK')),
    ADD CONSTRAINT member_language_check CHECK (language IN ('ID', 'EN'));

-- Profile photo, compressed by the app. Its own table so loading a member never reads the image.
CREATE TABLE idp.member_photo (
    member_id    varchar(255) NOT NULL REFERENCES idp.member (id),
    content      bytea        NOT NULL,
    content_type varchar(20)  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    CONSTRAINT member_photo_pkey PRIMARY KEY (member_id),
    CONSTRAINT member_photo_type_check CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp'))
);
