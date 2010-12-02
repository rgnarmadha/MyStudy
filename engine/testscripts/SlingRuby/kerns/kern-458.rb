#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
require 'sling/message'
include SlingSearch
include SlingMessage

class TC_Kern458Test < Test::Unit::TestCase
  include SlingTest
  

  
  def test_getdev
    # We create a test site.
    m = Time.now.to_i.to_s
    res = @s.execute_get(@s.url_for("/dev/"))
    assert_equal("text/html",res.content_type())
  end

end

