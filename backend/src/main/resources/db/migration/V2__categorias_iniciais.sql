INSERT INTO categorias (nome) VALUES
  ('Acessos e contas'),
  ('Equipamentos'),
  ('Sistemas e aplicativos'),
  ('Rede e internet'),
  ('Outros')
ON CONFLICT (nome) DO NOTHING;
