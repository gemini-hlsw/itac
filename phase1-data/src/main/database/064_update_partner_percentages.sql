-- Update partner percentage_shares in accordance with reduced staff time.

update partners set percentage_share =  2 where abbreviation = 'AR';
update partners set percentage_share =  5 where abbreviation = 'AU';
update partners set percentage_share =  4 where abbreviation = 'BR';
update partners set percentage_share = 13 where abbreviation = 'CA';
update partners set percentage_share = 10 where abbreviation = 'CL';
update partners set percentage_share =  5 where abbreviation = 'GeminiStaff';
update partners set percentage_share = 10 where abbreviation = 'UH';
update partners set percentage_share = 19 where abbreviation = 'UK';
update partners set percentage_share = 42 where abbreviation = 'US';
