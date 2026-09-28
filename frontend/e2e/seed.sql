-- Somente ambiente dev descartável. O login dev não permite elevar perfil.
INSERT INTO usuarios (nome, email, perfil, ativo)
VALUES ('Agente fictício E2E', 'agente-e2e@exemplo.invalid', 'TI_AGENTE', true)
ON CONFLICT (lower(email)) DO UPDATE SET perfil='TI_AGENTE', ativo=true;
