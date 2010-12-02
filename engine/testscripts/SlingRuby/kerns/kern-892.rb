#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'

class TC_Kern892Test < Test::Unit::TestCase
  include SlingTest

  def test_anonymous_root_access
    res = @s.execute_get(@s.url_for("/.json"))
    assert_equal("200", res.code, "All users should be allowed to reach the root node")
  end

end
