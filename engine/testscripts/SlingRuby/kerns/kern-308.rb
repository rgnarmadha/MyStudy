#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/sling'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingSearch

class TC_Kern308Test < Test::Unit::TestCase
  include SlingTest


  def test_308
    @m = Time.now.to_i.to_s
	u = create_user("ian"+@m)
	n = create_user("nico"+@m)
	g1t = create_group("g-group1-"+@m)
	@s.switch_user(u)
	g = create_group("g-group"+@m)
        @log.info(g.details(@s))
	assert_not_nil(g,"Failed to create group node ")
        g.add_member(@s, n.name, "user")

        details = g.details(@s)
        assert(g.has_member(@s, n.name), "Expected member to be added")
  end

  def test_delegation
    @m = Time.now.to_i.to_s
    u1 = create_user("bob"+@m)
    u2 = create_user("sam"+@m)
    u3 = create_user("eve"+@m)
    @s.switch_user(u1)
    g1 = create_group("g-#{u1.name}")
    g1.add_member(@s, u1.name, "user")
    assert(g1.has_member(@s, u1.name), "Expected user to be a member of their group")
    @s.switch_user(u2)
    g2 = create_group("g-#{u2.name}")
    g2.add_member(@s, u2.name, "user")
    assert(g2.has_member(@s, u2.name), "Expected user to be a member of their group")
	
    res = g2.add_manager(@s, g1.name)
    assert_equal("200", res.code, "Expected to be able to make change to add the group manager in "+res.body)
    res = @s.execute_get(@s.url_for(Group.url_for(g2.name) + ".tidy.json"))
	@log.debug(res.body)
    @log.info "Delegated admin property updated"
    @s.switch_user(u1)
    res = g2.add_member(@s, u3.name, "user")
    assert_equal("200", res.code, "Expected to be able to make change")
    assert(g2.has_member(@s, u3.name), "Expected foreign user to be able to modify group")
    @s.switch_user(u3)
    res = g2.remove_member(@s, u2.name, "user")
    assert_equal("500", res.code, "Expected not to be able to make change") 
    assert(g2.has_member(@s, u2.name), "Expected foreign user not to be able to modify group")
  end

end


