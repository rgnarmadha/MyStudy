#!/usr/bin/ruby

require 'sling/sling'
require 'sling/search'
include SlingInterface
include SlingSearch

if $ARGV.size < 3
  puts "Usage: create_search.rb NODE_PATH (sql|xpath) TEMPLATE"
  exit 1
end

path = $ARGV[0]
language = $ARGV[1]
template = $ARGV[2]
@s = Sling.new()
@sm = SearchManager.new(@s)
@sm.create_search_template(path, language, template)

