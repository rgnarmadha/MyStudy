#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingSearch

class TC_Kern538Test < Test::Unit::TestCase
  include SlingTest

  #
  # Creating a tree of nodes by posting a block of JSON.
  #

  def test_normal
    m = Time.now.to_f.to_s.gsub('.', '')
    treeuser = create_user("treeuser1#{m}")
    @s.switch_user(treeuser)

    # Create foo node in private store
    @s.execute_post(@s.url_for("#{treeuser.private_path_for(@s)}/foo"), {"foo" => "bar"})

    # Create the default tree
    jsonRes = create_tree(default_tree(), "#{treeuser.private_path_for(@s)}/foo")

    #Assertions
    default_asserts(jsonRes)
  end

  def test_with_jcr_in_property
    m = Time.now.to_f.to_s.gsub('.', '')
    treeuser = create_user("treeuser2#{m}")
    @s.switch_user(treeuser)

    # Create foo node in private store
    @s.execute_post(@s.url_for("#{treeuser.private_path_for(@s)}/foo"), {"foo" => "bar"})

    # Create the default tree
    tree = default_tree()
    tree["foo"]["jcr:primaryType"] = "nt:file"
    jsonRes = create_tree(tree, "#{treeuser.private_path_for(@s)}/foo")

    #Assertions
    default_asserts(jsonRes)
  end

  def test_noneExistingResource
    m = Time.now.to_f.to_s.gsub('.', '')
    treeuser = create_user("treeuser3#{m}")
    @s.switch_user(treeuser)

    # Create the default tree
    jsonRes = create_tree(default_tree(), "#{treeuser.private_path_for(@s)}/test")

    #Assertions
    default_asserts(jsonRes)
  end

  def test_accessdenied
    m = Time.now.to_f.to_s.gsub('.', '')
    treeuser = create_user("treeuser4#{m}")
    @s.switch_user(treeuser)

    # Create the default tree
    tree = default_tree()

    # Actual parameters we're sending in the request.
    parameters = {
      ":operation" => "createTree",
      "tree" => JSON.generate(tree)
    }

    # Execute the post
    res = @s.execute_post(@s.url_for("foo/bar"), parameters)

    assert_equal(res.code.to_i, 500, "Expected to fail.")

  end

  def test_withdelete
    m = Time.now.to_f.to_s.gsub('.', '')
    treeuser = create_user("treeuser5#{m}")
    @s.switch_user(treeuser)

    # Create the default tree
    tree = default_tree()

    parameters = {
      ":operation" => "createTree",
      "tree" => JSON.generate(tree),
      "delete" => "1"
    }

    # Create a node that needs to be deleted.
    res = @s.execute_post(@s.url_for("#{treeuser.private_path_for(@s)}/test"), {"toDelete" => "true"})
    assert_equal(201, res.code.to_i, "Expected to create a node.")

    # Execute the createTree post
    jsonRes = create_tree(default_tree(), "#{treeuser.private_path_for(@s)}/test", 5, "1")

    #Assertions
    default_asserts(jsonRes)
  end

  def default_asserts(jsonRes)
    assert_not_nil(jsonRes, "Expected to have some properties ")
    assert_not_nil(jsonRes["foo"], "Expected to find the Foo Element" )
    assert_not_nil(jsonRes["foo"]["bar1"], "Expected to find the Bar1 Element" )
    assert_not_nil(jsonRes["foo"]["bar2"], "Expected to find the Bar2 Element" )
    assert_equal(jsonRes["foo"]["title"], "Foo", "Expected to get 'Foo' as title.")
    assert_equal(jsonRes["foo"]["bar1"]["unit"], 1, "Expexted to get a childnode 'bar1' with property unit of '1.5'.")
    assert_equal(jsonRes["foo"]["bar1"]["title"], "First bar", "Expexted to get a childnode 'bar1' with property title of 'First bar'.")
    assert_equal(jsonRes["foo"]["bar2"]["unit"], 2.5, "Expexted to get a childnode 'bar2' with property unit of '2.5'.")
    assert_equal(jsonRes["foo"]["bar2"]["title"], "Second bar", "Expexted to get a childnode 'bar2' with property title of 'Second bar'.")
  end

  def default_tree()
    # Our tree should exist out of a node 'foo' with two childnodes 'bar1' and 'bar2'
    tree = {
        "foo" => {
            "title" => "Foo",
            "bar1" => {
                "title" => "First bar",
                "unit" => 1
        },
            "bar2" => {
                "title" => "Second bar",
                "unit" => 2.5
        }
      }
    }

    return tree
  end

  def create_tree(tree, url, levels = 5, delete = "0")

    # Actual parameters we're sending in the request.
    parameters = {
      ":operation" => "createTree",
      "tree" => JSON.generate(tree),
      "delete" => delete
    }

    # Execute the post
    postRes = @s.execute_post(@s.url_for(url), parameters)
    assert_equal("200", postRes.code, "Failed to create tree structure")

    # Return the result of that node
    res = @s.execute_get(@s.url_for("#{url}.#{levels}.json"))
    if ( res.code == "200" )
      return JSON.parse(res.body)
    else
      return JSON.parse("{}")
    end
  end

end

