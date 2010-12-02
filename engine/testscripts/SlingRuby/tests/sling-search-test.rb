#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_MySearchTest < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @sm = SearchManager.new(@s)
  end

  def test_node_search
    m = Time.now.to_i.to_s
    nodelocation = "some/test/location#{m}"
    create_node(nodelocation, { "a" => "anunusualstring", "b" => "bar" })
    result = @sm.search_for("anunusualstring")
    assert_not_nil(result, "Expected result back")
    nodes = result["results"]
    assert_equal(1, nodes.size, "Expected one matching node")
    assert_equal("bar", nodes[0]["b"], "Expected data to be loaded")
  end

  def test_user_search
    m = Time.now.to_i.to_s
    username = "unusualuser#{m}"
    create_user(username, "#{username}-firstname", "#{username}-lastname")

    result = @sm.search_for_user("#{username}")
    assert_not_nil(result, "Expected result back")
    users = result["results"]
    assert_equal(1, users.size, "Expected one matching user [username]")
    assert_equal(username, users[0]["rep:userId"], "Expected user to match username")

    result = @sm.search_for_user("#{username}-firstname")
    assert_not_nil(result, "Expected result back")
    users = result["results"]
    assert_equal(1, users.size, "Expected one matching user [firstname]")
    assert_equal(username, users[0]["rep:userId"], "Expected user to match firstname")

    result = @sm.search_for_user("#{username}-lastname")
    assert_not_nil(result, "Expected result back")
    users = result["results"]
    assert_equal(1, users.size, "Expected one matching user [lastname]")
    assert_equal(username, users[0]["rep:userId"], "Expected user to match lastname")
  end

end


