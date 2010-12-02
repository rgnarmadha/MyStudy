#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling.rb'
require 'test/unit.rb'
require 'logger'
include SlingInterface
include SlingUsers

class TC_MyTest < Test::Unit::TestCase

  def setup
    @s = Sling.new()
    @um = UserManager.new(@s)
    #@s.debug = true
    m = Time.now.to_i.to_s
    @test_node = "some_test_node"+m
    @s.delete_node(@test_node)
    @log = Logger.new(STDOUT)
    @log.level = Logger::WARN
  end

  def teardown
    @s.switch_user(User.admin_user)
    @s.delete_node(@test_node)
  end

  def test_ownership_privs

    # Set up user and group
    @log.info("Creating test user ")
    user = @um.create_test_user(10)
    user2 = @um.create_test_user(11)
    assert_not_nil(user, "Expected user to be created")
    assert_not_nil(user2, "Expected user2 to be created")
    # assume already exists
    owner = Group.new("owner")
    #@s.debug = true
    @log.info("Updating dynamic properties for owner just in case ")
    @s.update_properties(owner, { "dynamic" => "true" })
    assert_not_nil(owner, "Expected owner group to be created")
    @log.info("Owner group created fully")

    # Create admin-owned parent node
    @log.info("Creating admin owned test node #{@test_node} ")
    @s.create_node(@test_node, { "jcr:mixinTypes" => "mix:created", "foo" => "bar", "baz" => "jim" })
    props = @s.get_node_props(@test_node)
    @log.debug("Got properties of the node as: "+@s.get_node_props_json(@test_node))
    assert_equal("bar", props["foo"])
    @log.info(" Clear the acl ")
    @s.clear_acl(@test_node)
    acl = @s.get_node_acl(@test_node)
    assert(acl.size == 0, "Expected ACL to be cleared")


    @s.set_node_acl_entries(@test_node, user, { "jcr:write" => "granted",
                                                "jcr:addChildNodes" => "granted",
                                                "jcr:readAccessControl" => "granted",
                                                "jcr:modifyProperties" => "granted" ,
												"jcr:nodeTypeManagement" => "granted" })
    @log.debug("ACL For test Node is #{@test_node} "+ @s.get_node_acl_json(@test_node))

    # Switch to unprivileged user, create child node owned by user
    @s.switch_user(user)
    child_node = "#{@test_node}/bar"
    @s.create_node(child_node, {  "jcr:mixinTypes" => "mix:created", "bob" => "cat" })
    @log.debug("Got properties of the child node as: "+@s.get_node_props_json(child_node))
    @log.debug("ACL For test Child Node is  #{child_node}"+ @s.get_node_acl_json(child_node))


    # Switch to admin, add "modifyAccessControl" priv to owner on new child node
    @s.switch_user(User.admin_user)
    @s.set_node_acl_entries(child_node, owner, "jcr:modifyAccessControl" => "granted")

    # Switch back to unprivileged user and exercise the owner grant
    @s.switch_user(user)
    @log.debug("As Unprivileged User #{user} properties are ")
    @s.get_node_props_json(child_node)
    @log.debug("As Unprivileged User ACLs are ")
    @s.get_node_acl_json(child_node)
    @log.info("Modifying ACL as unpivileged User #{user} ")
    res = @s.set_node_acl_entries(child_node, user, { "jcr:addChildNodes" => "denied" })
    assert_equal(200, res.code.to_i, "Expected to be able to modify ACL")
    @log.info("As Unprivileged User ACLs are set as #{user}  ")
    @log.debug @s.get_node_acl_json(@test_node)

    # Switch to a different unprivileged user and assert owner grant is not in effect
    @s.switch_user(user2)
    @log.info("As non owner checking  #{user2.name} ")
    res = @s.set_node_acl_entries(child_node, user2, { "jcr:addChildNodes" => "granted" })
    assert_equal(500, res.code.to_i, "Expected not to be able to modify ACL")
  end

end


