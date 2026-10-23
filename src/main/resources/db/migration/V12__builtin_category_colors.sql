-- Distinct colors for the built-in categories, so charts per category (finance summary) can tell them apart.
UPDATE transaction_finance.finance_category SET color = '#22C55E' WHERE id = 'default-SALARY';
UPDATE transaction_finance.finance_category SET color = '#14B8A6' WHERE id = 'default-PASSIVE_INCOME';
UPDATE transaction_finance.finance_category SET color = '#3B82F6' WHERE id = 'default-INVESTMENT';
UPDATE transaction_finance.finance_category SET color = '#84CC16' WHERE id = 'default-OTHERS_INCOME';
UPDATE transaction_finance.finance_category SET color = '#EF4444' WHERE id = 'default-FOOD_AND_BEVERAGE';
UPDATE transaction_finance.finance_category SET color = '#F97316' WHERE id = 'default-TRANSPORTATION';
UPDATE transaction_finance.finance_category SET color = '#F59E0B' WHERE id = 'default-UTILITIES_AND_BILLS';
UPDATE transaction_finance.finance_category SET color = '#8B5CF6' WHERE id = 'default-ENTERTAINMENT';
UPDATE transaction_finance.finance_category SET color = '#64748B' WHERE id = 'default-OTHERS_EXPENSE';
