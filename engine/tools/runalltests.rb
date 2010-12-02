#!/usr/bin/env ruby

require 'find'

START_DIR = Dir.pwd

Find.find(".") do |path|
  if FileTest.directory?(path)
    next
  else
    if File.basename(path) == "testall.rb"
      Dir.chdir(File.dirname(path))
      load('./' + File.basename(path), true)
      Dir.chdir(START_DIR)
    end
  end
end

