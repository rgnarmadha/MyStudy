#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
include SlingContacts

class TC_Kern288Test < Test::Unit::TestCase
  include SlingTest

  def test_connection_details
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, "follower")
    pending = cm.get_pending
    assert(pending["results"].size == 1, "Expected pending invitation")
    res = cm.cancel_invitation(u2.name)
    assert_equal("200", res.code, "Expected cancel to succeed")
    pending = cm.get_pending
    assert(pending["results"].size == 0, "Expected no pending invitation")
  end

end


