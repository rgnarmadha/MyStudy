#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern307Test < Test::Unit::TestCase
  include SlingTest

  def create_test_node_with_permissions(user, m)
    @s.switch_user(SlingUsers::User.admin_user)
    node = create_node("some/test/path#{m}", {})
    writers = create_group("g-test-writers-#{m}")
    writers.add_member(@s, user.name, "user")
    @s.set_node_acl_entries(node, writers, { "jcr:addChildNodes" => "granted", "jcr:write" => "granted" })
    return node
  end

  def add_test_child_node(user, node)
    @s.switch_user(user)
    child = create_node("#{node}/child", {})
    assert_not_nil(child, "Expected node to be created")
  end

  def test_dirty_acl_cache
    m = Time.now.to_i.to_s
    #@s.log = true
    randomuser = create_user("randomuser#{m}")

    node1 = create_test_node_with_permissions(randomuser, "#{m}1")
    add_test_child_node(randomuser, node1)

    node2 = create_test_node_with_permissions(randomuser, "#{m}2")
    add_test_child_node(randomuser, node2)
  end

  def test_clean_acl_cache
    m = Time.now.to_i.to_s
    #@s.log = true
    randomuser = create_user("randomuser#{m}")

    node1 = create_test_node_with_permissions(randomuser, "#{m}1")
    node2 = create_test_node_with_permissions(randomuser, "#{m}2")

    add_test_child_node(randomuser, node1)
    add_test_child_node(randomuser, node2)
  end

end


