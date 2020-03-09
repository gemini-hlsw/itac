#!/usr/local/bin/ruby

# SQL injection is fun! This script is not safe.
# Generates a ton of update statments that fix the database fields which picked up HTML cruft during editing.

require 'rubygems'
require 'pg'
require 'htmlentities'

def fix_string(input)
  grr = (input =~ /&nbsp;/)

  wreckme = String.new input
  
  wreckme.gsub!(/&nbsp;/,' ') # nbsp is not whitespace and won't be stripped.  Get it ready to be killed with fire.
  wreckme.gsub!(/<\/?[^>]*>/, "")
  wreckme.gsub!('p.p1 {margin: 0.0px 0.0px 0.0px 0.0px; font: 12.0px Verdana}', '')
  intermediate = HTMLEntities.new.decode(wreckme)
  output = intermediate.strip
  output
end

Conn = PGconn.connect("localhost", 5432, '', '', "itac_dev", "itac", "")

def clean_field_in_table_with_pk_and_optional_qualifier(field, table, pk, qualifier = nil)
  puts "-- cleaning #{ field } in  #{ table } with where clause #{ qualifier } and pk #{ pk }"
  unless qualifier.nil?
    res = Conn.exec("select #{ pk }, #{ field } from #{ table } where #{ qualifier }")
  else
    res = Conn.exec("select #{ pk }, #{ field } from #{ table }")
  end

  res.each do |row|
    stripped = fix_string row[field]
    
    if (stripped != row[field])
      puts "/* '#{ row[field] }' originally. */"
      puts "update #{ table } set #{ field } = E'#{ stripped.gsub("'","''").gsub("\\","\\\\") }' where #{ pk } = '#{ row[pk] }';"  
    end
  end
end

clean_field_in_table_with_pk_and_optional_qualifier('gemini_contact_scientist_email','p1_itac_extensions','extension_id') 
clean_field_in_table_with_pk_and_optional_qualifier('itac_comment','p1_itac_extensions','extension_id') 
clean_field_in_table_with_pk_and_optional_qualifier('gemini_comment','p1_itac_extensions','extension_id') 
clean_field_in_table_with_pk_and_optional_qualifier('partner_comment','p1_tac_extensions','extension_id') 
clean_field_in_table_with_pk_and_optional_qualifier('partner_ranking','p1_tac_extensions','extension_id') 
clean_field_in_table_with_pk_and_optional_qualifier('partner_support_email','p1_tac_extensions','extension_id') 

