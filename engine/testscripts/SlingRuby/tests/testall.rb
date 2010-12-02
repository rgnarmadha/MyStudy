#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'logger'

SlingTest.setLogLevel(Logger::ERROR)

Dir.foreach(".") do |path|
  if /.*\-test.rb/.match(File.basename(path))
    require path
  end
end
