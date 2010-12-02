#!/usr/bin/ruby

require 'sling/sling'
require 'sling/users'
include SlingInterface
include SlingUsers

if $ARGV.size < 1
  puts "Usage: delete_users.rb USERNAME [USERNAME ...]"
  exit 1
end

@s = Sling.new()
@um = UserManager.new(@s)

$ARGV.each do |user|
  puts @um.delete_user(user)
end

