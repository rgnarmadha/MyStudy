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


class TC_Kern759Test < Test::Unit::TestCase
  include SlingTest
  
  
  def test_private_group
    # Create a couple of user who are connected
    m = Time.now.to_i.to_s

    manager = create_user("user-manager-#{m}")
    viewer = create_user("user-viewer-#{m}")
    other = create_user("user-other-#{m}")
    admin = User.new("admin","admin")

    # Create a group
    contactsgroup = create_group("g-test-group-#{m}")

    @s.switch_user(other)

    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
    assert_equal("200",res.code)

    @s.switch_user(admin)

    contactsgroup.add_manager(@s, manager.name)
    contactsgroup.add_viewer(@s, viewer.name)
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
    assert_equal("200",res.code)
    @log.debug(res.body)


    @s.switch_user(other)

    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
    assert_equal("404",res.code, res.body)
    res = contactsgroup.update_properties(@s, { "testing" => "Should Fail to Update" } )
    assert_equal("500",res.code, res.body)

    @s.switch_user(viewer)
    #@s.debug = true
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
    #@s.debug = false
    assert_equal("200",res.code, res.body)
    res = contactsgroup.update_properties(@s, { "testing" => "Should Fail to Update" } ) 
    assert_equal("500",res.code, res.body)

    @s.switch_user(manager)
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
    assert_equal("200",res.code, res.body)
    res = contactsgroup.update_properties(@s, { "testing" => "Should Be Ok" } ) 
    assert_equal("200",res.code, res.body)
  end
  
end
