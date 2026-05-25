SET search_path TO food;

INSERT INTO sf_restaurant (name, category, address, rating, distance_km)
SELECT '海底捞（万象城店）', 'hotpot', '万象城', 4.8, 1.10
WHERE NOT EXISTS (SELECT 1 FROM sf_restaurant WHERE name = '海底捞（万象城店）');

INSERT INTO sf_restaurant (name, category, address, rating, distance_km)
SELECT '喜茶（万象城店）', 'drink', '万象城', 4.6, 1.50
WHERE NOT EXISTS (SELECT 1 FROM sf_restaurant WHERE name = '喜茶（万象城店）');

INSERT INTO sf_restaurant (name, category, address, rating, distance_km)
SELECT '麦当劳（万象城店）', 'fast', '万象城', 4.5, 1.80
WHERE NOT EXISTS (SELECT 1 FROM sf_restaurant WHERE name = '麦当劳（万象城店）');

INSERT INTO sf_restaurant (name, category, address, rating, distance_km)
SELECT '木屋烤肉（科技园店）', 'bbq', '科技园', 4.7, 2.20
WHERE NOT EXISTS (SELECT 1 FROM sf_restaurant WHERE name = '木屋烤肉（科技园店）');
