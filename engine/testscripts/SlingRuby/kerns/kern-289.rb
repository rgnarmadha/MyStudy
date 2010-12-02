#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
include SlingContacts

class TC_Kern289Test < Test::Unit::TestCase
  include SlingTest

  def test_connection_details
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    
    home1 = u1.home_path_for(@s)
    
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, "follower")
    #@s.debug = true
    contacts = cm.get_all()
    #@s.debug = false
    assert_not_nil(contacts)
    assert_not_nil(contacts["results"]," No Contacts found")
    assert_not_nil(contacts["results"][0], " No Contacts found ")
    types = contacts["results"][0]["details"]["sakai:types"]
    assert_not_nil(types, "Expected type to be stored")
    types = [*types]
    assert_equal("follower", types.first, "Expected type to be 'follower'")
  end

end


