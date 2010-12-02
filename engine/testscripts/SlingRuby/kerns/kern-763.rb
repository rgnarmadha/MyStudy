#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
include SlingSearch
include SlingUsers
include SlingContacts


class TC_Kern763Test < Test::Unit::TestCase
  include SlingTest
  
  
  def test_permissions
    # Create a couple of user who are connected
    m = Time.now.to_i.to_s
    u1 = create_user("user-one-#{m}")
    u2 = create_user("user-two-#{m}")
    u3 = create_user("user-three-#{m}")
    # Connections
    cm = ContactManager.new(@s)
    create_connection(u1, u2, cm)
    
    # User 1 will share the node at ~/public/foo  
    @s.switch_user(u1)
    contactsgroup = Group.new("g-contacts-user-one-#{m}")
    everyone = Group.new("everyone")
    path = u1.home_path_for(@s) + "/public/foo"
    @s.create_node(path + "/bar", {"foo" => "bar"})
    @s.set_node_acl_entries(path, contactsgroup, {"jcr:read" => "granted"})
    @s.set_node_acl_entries(path, everyone, {"jcr:read" => "denied"})
    
    # Make sure user 2 has access
    @s.switch_user(u2)
    res = @s.execute_get(@s.url_for(path) + ".infinity.json")
    json = JSON.parse(res.body)
    assert_equal("bar", json["bar"]["foo"])
    
    # Make sure user 2 does not have access (by checking 404)
    @s.switch_user(u3)
    res = @s.execute_get(@s.url_for(path) + ".infinity.json")
    assert_equal(404, res.code.to_i)
    
    # Delete user2 as a contact and ensure that he doesn't have access anymore.
    @s.switch_user(u1)
    cm.remove_contact(u2.name)
    @s.switch_user(u2)
    res = @s.execute_get(@s.url_for(path) + ".infinity.json")
    assert_equal(404, res.code.to_i)
  end
  
  def create_connection(baseUser, otherUser, cm) 
    @s.switch_user(baseUser)
    cm.invite_contact(otherUser.name, "follower")
    @s.switch_user(otherUser)
    cm.accept_contact(baseUser.name)
    
  end
  
end

