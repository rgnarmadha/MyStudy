#!/usr/bin/ruby

require 'sling/sling'
include SlingInterface

if $ARGV.size < 1
  puts "Usage: delete-node.rb NODE_PATH"
  exit 1
end

path = $ARGV[0]
@s = Sling.new()
@s.delete_node(path)

