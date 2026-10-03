-- ════════════════════════════════════════════════════════════════════════
-- Smart Shifting — item catalog seed (70 rows)
-- ════════════════════════════════════════════════════════════════════════
-- MySQL / MariaDB syntax; adjust quoting for Postgres.
--
-- volume_cft is the space the item takes ON A LOADED VEHICLE. It is NOT
-- length x width x height, and must not be computed from it. The l/w/h columns
-- exist only so the app can show 'approx 6.5 x 3 x 3 ft' to the customer.
--
-- INVARIANT: for any item over ~3 cft, volume_cft < length*width*height.
-- A rigid object cannot take more room than its own bounding box. Small items
-- are the exception - a laptop needs a padded bag.

INSERT INTO moving_items
  (item_id, name, category, icon, volume_cft, weight_kg, length_ft, width_ft,
   height_ft, is_bulky, is_fragile, is_preset, hint, sort_order, is_active)
VALUES
  ('food_tiffin', 'Tiffin / lunch box', 'food', '🍱', 0.4, 1.5, 1.0, 0.8, 0.8, 0, 1, 0, 'Home food', 1, 1),
  ('food_meal', 'Packed meal', 'food', '🍛', 0.3, 1.0, 1.0, 0.8, 0.5, 0, 1, 0, 'Restaurant order', 2, 1),
  ('food_cake', 'Cake', 'food', '🎂', 1.2, 2.0, 1.0, 1.0, 1.0, 0, 1, 0, 'Boxed, keep flat', 3, 1),
  ('food_sweets', 'Sweets / mithai box', 'food', '🍬', 0.6, 2.0, 1.0, 1.0, 0.5, 0, 1, 0, NULL, 4, 1),
  ('food_groceries', 'Grocery bag', 'food', '🛍️', 1.5, 8.0, 1.2, 1.0, 1.2, 0, 0, 0, NULL, 5, 1),
  ('food_vegetables', 'Vegetables / fruit crate', 'food', '🥬', 3.0, 15.0, 2.0, 1.3, 1.0, 0, 0, 0, NULL, 6, 1),
  ('food_water_can', 'Water can', 'food', '💧', 1.5, 20.0, 1.0, 1.0, 1.5, 0, 0, 0, '20 litre', 7, 1),
  ('food_milk_crate', 'Milk / dairy crate', 'food', '🥛', 2.0, 18.0, 1.7, 1.2, 1.0, 0, 1, 0, NULL, 8, 1),
  ('food_catering', 'Catering vessel', 'food', '🍲', 6.0, 25.0, 2.0, 2.0, 1.5, 0, 1, 0, 'Large degh', 9, 1),
  ('small_documents', 'Documents / envelope', 'small_items', '📄', 0.2, 0.5, 1.2, 0.9, 0.2, 0, 0, 0, NULL, 10, 1),
  ('small_keys', 'Keys / small packet', 'small_items', '🔑', 0.1, 0.3, 0.5, 0.4, 0.3, 0, 0, 0, NULL, 11, 1),
  ('small_medicines', 'Medicines', 'small_items', '💊', 0.4, 1.0, 1.0, 0.8, 0.5, 0, 1, 0, NULL, 12, 1),
  ('small_laptop', 'Laptop / tablet', 'small_items', '💻', 0.8, 3.0, 1.3, 1.0, 0.4, 0, 1, 0, NULL, 13, 1),
  ('small_mobile', 'Mobile / gadget', 'small_items', '📱', 0.2, 0.5, 0.7, 0.5, 0.3, 0, 1, 0, NULL, 14, 1),
  ('small_gift', 'Gift / flowers', 'small_items', '🎁', 1.5, 3.0, 1.3, 1.0, 1.3, 0, 1, 0, NULL, 15, 1),
  ('small_clothes', 'Clothes packet', 'small_items', '👕', 1.5, 4.0, 1.5, 1.0, 0.8, 0, 0, 0, NULL, 16, 1),
  ('small_book_parcel', 'Books parcel', 'small_items', '📚', 1.5, 10.0, 1.3, 1.0, 1.0, 0, 0, 0, NULL, 17, 1),
  ('small_spare_part', 'Spare part / tool', 'small_items', '🔧', 1.5, 8.0, 1.5, 1.0, 0.8, 0, 0, 0, NULL, 18, 1),
  ('small_carton', 'Single carton', 'small_items', '📦', 3.0, 10.0, 1.7, 1.3, 1.3, 0, 0, 0, 'Standard courier box', 19, 1),
  ('sofa_3', 'Sofa', 'furniture', '🛋️', 35.0, 45.0, 6.5, 3.0, 3.0, 1, 0, 0, '3 seater', 20, 1),
  ('sofa_2', 'Sofa', 'furniture', '🛋️', 24.0, 32.0, 4.5, 3.0, 3.0, 1, 0, 0, '2 seater', 21, 1),
  ('sofa_l', 'L-shape sofa', 'furniture', '🛋️', 55.0, 70.0, 8.0, 6.0, 3.0, 1, 0, 0, 'Corner set', 22, 1),
  ('bed_double', 'Double bed', 'furniture', '🛏️', 30.0, 60.0, 6.5, 5.0, 2.5, 1, 0, 0, 'With headboard', 23, 1),
  ('bed_single', 'Single bed', 'furniture', '🛏️', 18.0, 35.0, 6.5, 3.0, 2.0, 1, 0, 0, NULL, 24, 1),
  ('mattress', 'Mattress', 'furniture', '🛌', 14.0, 25.0, 6.5, 5.0, 0.7, 1, 0, 0, 'Queen / king', 25, 1),
  ('wardrobe', 'Wardrobe', 'furniture', '🚪', 38.0, 70.0, 4.0, 2.0, 6.5, 1, 0, 0, '2 door', 26, 1),
  ('almirah', 'Steel almirah', 'furniture', '🗄️', 26.0, 80.0, 3.0, 1.7, 6.0, 1, 0, 0, NULL, 27, 1),
  ('dining_table', 'Dining table', 'furniture', '🍽️', 22.0, 40.0, 5.0, 3.0, 2.5, 1, 0, 0, 'Top only', 28, 1),
  ('chair', 'Chair', 'furniture', '🪑', 6.0, 7.0, 1.5, 1.5, 3.0, 0, 0, 0, NULL, 29, 1),
  ('study_table', 'Study table', 'furniture', '🖊️', 14.0, 25.0, 4.0, 2.0, 2.5, 0, 0, 0, NULL, 30, 1),
  ('bookshelf', 'Bookshelf', 'furniture', '📚', 16.0, 35.0, 3.0, 1.2, 5.0, 0, 0, 0, NULL, 31, 1),
  ('shoe_rack', 'Shoe rack', 'furniture', '👟', 9.0, 14.0, 2.5, 1.2, 3.0, 0, 0, 0, NULL, 32, 1),
  ('center_table', 'Center table', 'furniture', '🛎️', 8.0, 15.0, 3.0, 2.0, 1.5, 0, 0, 0, NULL, 33, 1),
  ('tv_unit', 'TV unit', 'furniture', '📺', 10.0, 30.0, 4.0, 1.5, 2.0, 0, 0, 0, NULL, 34, 1),
  ('mirror', 'Mirror / dressing table', 'furniture', '🪞', 12.0, 22.0, 3.0, 1.5, 5.0, 1, 1, 0, NULL, 35, 1),
  ('fridge_double', 'Refrigerator', 'appliances', '🧊', 28.0, 90.0, 2.5, 2.5, 6.0, 1, 0, 0, 'Double door', 36, 1),
  ('fridge_single', 'Refrigerator', 'appliances', '🧊', 16.0, 55.0, 2.0, 2.0, 5.0, 1, 0, 0, 'Single door', 37, 1),
  ('washing_machine', 'Washing machine', 'appliances', '🌀', 15.0, 65.0, 2.2, 2.2, 3.5, 1, 0, 0, NULL, 38, 1),
  ('ac_split', 'AC unit', 'appliances', '❄️', 12.0, 45.0, 3.5, 2.5, 2.0, 1, 0, 0, 'Split, both units', 39, 1),
  ('tv', 'Television', 'appliances', '📺', 8.0, 18.0, 4.5, 0.8, 3.0, 1, 1, 0, 'Boxed', 40, 1),
  ('microwave', 'Microwave / oven', 'appliances', '🍲', 3.5, 15.0, 2.0, 1.5, 1.3, 0, 0, 0, NULL, 41, 1),
  ('gas_stove', 'Gas stove + cylinder', 'appliances', '🔥', 6.0, 25.0, 2.0, 1.5, 2.0, 0, 0, 0, NULL, 42, 1),
  ('water_purifier', 'Water purifier', 'appliances', '💧', 2.5, 12.0, 1.3, 1.0, 1.7, 0, 1, 0, NULL, 43, 1),
  ('cooler', 'Air cooler', 'appliances', '🌬️', 14.0, 20.0, 2.0, 2.0, 3.5, 1, 0, 0, NULL, 44, 1),
  ('geyser', 'Geyser', 'appliances', '♨️', 4.0, 14.0, 1.5, 1.5, 2.0, 0, 0, 0, NULL, 45, 1),
  ('box_small', 'Box', 'boxes', '📦', 2.0, 8.0, 1.3, 1.3, 1.3, 0, 0, 0, 'Small', 46, 1),
  ('box_medium', 'Box', 'boxes', '📦', 4.0, 15.0, 1.7, 1.7, 1.7, 0, 0, 0, 'Medium', 47, 1),
  ('box_large', 'Box', 'boxes', '📦', 7.0, 22.0, 2.0, 2.0, 2.0, 0, 0, 0, 'Large', 48, 1),
  ('suitcase', 'Suitcase', 'boxes', '🧳', 3.2, 18.0, 2.3, 1.5, 1.0, 0, 0, 0, NULL, 49, 1),
  ('bag', 'Bag / sack', 'boxes', '🎒', 3.0, 12.0, 1.7, 1.2, 1.5, 0, 0, 0, NULL, 50, 1),
  ('trunk', 'Trunk', 'boxes', '🧰', 6.0, 25.0, 3.0, 1.7, 1.5, 0, 0, 0, NULL, 51, 1),
  ('plants', 'Plant / pot', 'boxes', '🪴', 3.0, 10.0, 1.3, 1.3, 2.5, 0, 1, 0, NULL, 52, 1),
  ('cycle', 'Bicycle', 'boxes', '🚲', 10.0, 15.0, 6.0, 1.5, 3.5, 1, 0, 0, NULL, 53, 1),
  ('home_1rk', '1 RK', 'full_house', '🏠', 110.0, 320.0, NULL, NULL, NULL, 1, 0, 1, 'Studio / single room', 54, 1),
  ('home_1bhk', '1 BHK', 'full_house', '🏠', 175.0, 520.0, NULL, NULL, NULL, 1, 0, 1, 'Typical 1 bedroom', 55, 1),
  ('home_2bhk', '2 BHK', 'full_house', '🏡', 300.0, 900.0, NULL, NULL, NULL, 1, 0, 1, 'Typical 2 bedroom', 56, 1),
  ('home_3bhk', '3 BHK', 'full_house', '🏘️', 450.0, 1350.0, NULL, NULL, NULL, 1, 0, 1, 'Typical 3 bedroom', 57, 1),
  ('home_4bhk', '4 BHK / villa', 'full_house', '🏰', 620.0, 1900.0, NULL, NULL, NULL, 1, 0, 1, 'Large home', 58, 1),
  ('office_desk', 'Office desk', 'business', '🖥️', 20.0, 35.0, 5.0, 2.5, 2.5, 1, 0, 0, NULL, 59, 1),
  ('office_chair', 'Office chair', 'business', '💺', 8.0, 12.0, 2.0, 2.0, 3.5, 0, 0, 0, NULL, 60, 1),
  ('filing_cabinet', 'Filing cabinet', 'business', '🗃️', 11.0, 45.0, 1.5, 2.0, 4.0, 1, 0, 0, NULL, 61, 1),
  ('computer', 'Computer / monitor', 'business', '🖱️', 3.0, 12.0, 2.0, 1.0, 1.7, 0, 1, 0, NULL, 62, 1),
  ('printer', 'Printer', 'business', '🖨️', 5.0, 20.0, 2.0, 1.7, 1.5, 0, 1, 0, NULL, 63, 1),
  ('server_rack', 'Server rack', 'business', '🗄️', 24.0, 110.0, 2.5, 3.0, 6.0, 1, 1, 0, NULL, 64, 1),
  ('display_rack', 'Display rack', 'business', '🏪', 22.0, 40.0, 4.0, 1.5, 6.0, 1, 0, 0, NULL, 65, 1),
  ('counter', 'Counter table', 'business', '🧾', 25.0, 55.0, 6.0, 2.0, 3.5, 1, 0, 0, NULL, 66, 1),
  ('stock_carton', 'Stock carton', 'business', '📦', 4.0, 18.0, 1.7, 1.7, 1.7, 0, 0, 0, NULL, 67, 1),
  ('sack', 'Sack / gunny bag', 'business', '🧺', 3.0, 30.0, 2.5, 1.5, 1.0, 0, 0, 0, NULL, 68, 1),
  ('drum', 'Drum / barrel', 'business', '🛢️', 9.0, 60.0, 2.0, 2.0, 3.0, 1, 0, 0, NULL, 69, 1),
  ('pallet', 'Pallet', 'business', '🪵', 30.0, 250.0, 4.0, 4.0, 4.0, 1, 0, 0, NULL, 70, 1);
