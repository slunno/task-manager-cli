CREATE FUNCTION proteger_historico_chamado() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'DELETE' AND current_setting('helpdesk.retencao', true) = 'on' THEN
    RETURN OLD;
  END IF;
  RAISE EXCEPTION 'Histórico de chamado é imutável; exclusão somente pela retenção'
    USING ERRCODE = '42501';
END;
$$;

CREATE TRIGGER historico_chamado_imutavel
BEFORE UPDATE OR DELETE ON historico_chamado
FOR EACH ROW EXECUTE FUNCTION proteger_historico_chamado();
