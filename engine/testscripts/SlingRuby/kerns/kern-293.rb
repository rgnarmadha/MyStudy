#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern293Test < Test::Unit::TestCase
  include SlingTest

=begin
  def test_overwrite
    m = Time.now.to_i.to_s
    n1 = create_node("d1#{m}")
    n2 = create_node("d2#{m}")
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "copy",
                                                 ":dest" => n2 }) 
    assert_equal("412", res.code, "Expected not to be allowed to overwrite")
  end
=end

  def test_overwrite_with_replace
    m = Time.now.to_i.to_s
    n1 = create_node("a1#{m}")
    n2 = create_node("a2#{m}")
	res = @s.execute_get(@s.url_for("a1#{m}.json"))
    assert_equal("200", res.code, "Expected to find the node at  a1#{m} , but got "+res.body)
	@log.info("Got good response for  a1#{m} as "+res.body)
	res = @s.execute_get(@s.url_for("a2#{m}.json"))
    assert_equal("200", res.code, "Expected to find the node at  a2#{m} , but got "+res.body)
	@log.info("Got good response for  a2#{m} as "+res.body)
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "copy",
                                                 ":dest" => n2,
                                              ":replace" => "true" }) 
    assert_equal("200", res.code, "Expected to be allowed to overwrite, but got "+res.body)
  end

end


