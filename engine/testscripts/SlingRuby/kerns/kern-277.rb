#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingAuthz

class TC_Kern277Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @authz = SlingAuthz::Authz.new(@s)
    @m = Time.now.to_i.to_s
  end

  def test_group_deny
    path = "test/authztest/node" + @m
    group = create_group("g-group-" + @m)
    create_node(path, "testproperty" => "testvalue")
    @authz.grant(path, group.name, "jcr:write" => "denied")
    acl = @authz.getacl(path)
    @log.info "Got ace: #{acl.to_s}"
    ace = acl[group.name]
    assert_not_nil(ace["denied"], "Expected ACE for #{group.name} deny")
  end

  def test_deny_is_effective
    path = "test/authztest/node" + @m
    create_node(path, "testproperty" => "testvalue")
    group1 = create_group("g-group1-" + @m)
    group2 = create_group("g-group2-" + @m)
    user1 = create_user("user1-" + @m)
    user2 = create_user("user2-" + @m)
    group1.add_member(@s, user1.name, "user")
    #group1.add_member(@s, user2.name, "user")
    group2.add_member(@s, user2.name, "user")
    @authz.grant(path, group2.name, "jcr:write" => "denied")
    @authz.grant(path, group1.name, "jcr:write" => "granted")
    acl = @authz.getacl(path)
    @log.info "Got ace: #{acl.to_s}"
    @s.switch_user(user1)
    res = @s.update_node_props(path, "fish" => "cat")
    assert_equal("200", res.code, "Expected modification to succeed")
    assert_equal("cat", @s.get_node_props(path)["fish"], "Expected property to be updated")
    @s.switch_user(user2)
    res = @s.update_node_props(path, "dog" => "pig")
    assert_equal("500", res.code, "Expected modification to fail "+res.body)
    assert_nil(@s.get_node_props(path)["pig"], "Expected property to be absent")
  end

  def test_hierarchy_deny
    parent = "test/authztest/parent#{@m}"
    child = "#{parent}/node#{@m}"
    create_node(parent, "testproperty" => "testvalue")
    create_node(child, "testproperty2" => "testvalue2")
    group1 = create_group("g-group1-" + @m)
    group2 = create_group("g-group2-" + @m)
    user1 = create_user("user1-" + @m)
    user2 = create_user("user2-" + @m)
    group1.add_member(@s, user1.name, "user")
    group1.add_member(@s, user2.name, "user")
    group2.add_member(@s, user2.name, "user")
    @authz.grant(parent, group1.name, "jcr:write" => "granted")
    @authz.grant(child, group2.name, "jcr:write" => "denied")
    acl = @authz.getacl(child)
    @log.info "Got ace: #{acl.to_s}"
    @s.switch_user(user1)
    res = @s.update_node_props(child, "fish" => "cat")
    assert_equal("200", res.code, "Expected modification to succeed")
    assert_equal("cat", @s.get_node_props(child)["fish"], "Expected property to be updated")
    @s.switch_user(user2)
    res = @s.update_node_props(child, "dog" => "pig")
    assert_equal("500", res.code, "Expected modification to fail")
    assert_nil(@s.get_node_props(child)["pig"], "Expected property to be absent")
  end

end


