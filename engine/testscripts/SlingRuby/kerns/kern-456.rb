#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern456Test < Test::Unit::TestCase
  include SlingTest

  def test_site_node_deletion
    m = Time.now.to_i.to_s
    site_creator = create_user("testuser#{m}")
    non_member = create_user("otheruser#{m}")
    @s.switch_user(site_creator)
    site = @sm.create_site("sites", "Site test", "/testsite#{m}")
    sitepath = "sites" + "/testsite#{m}"
    @s.switch_user(non_member)
    res = @sm.delete_site(sitepath)
    assert_not_equal("200", res.code, "Expected not to delete site " + res.body)
    @s.switch_user(site_creator)
    res = @sm.delete_site(sitepath)
    assert_equal("200", res.code, "Expected to delete site " + res.body)
  end

end

