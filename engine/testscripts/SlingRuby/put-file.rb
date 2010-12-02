#!/usr/bin/ruby

require 'sling/sling'
include SlingInterface

if $ARGV.size < 2
  puts "Usage: put-file.rb NODE_PATH FILENAME"
  exit 1
end

path = $ARGV[0]
filename = $ARGV[1]
@s = Sling.new()
@s.create_file_node(path, "file", filename)

