#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
include SlingContacts

class TC_Kern345Test < Test::Unit::TestCase
  include SlingTest

  def test_asymmetric_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser1#{m}")
    u2 = create_user("otheruser1#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, [], ["follower"], ["leader"])
    check_contact_relationships(cm, "follower")
    @s.switch_user(u2)
    check_contact_relationships(cm, "leader")
  end

  def test_shared_and_asymmetric_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser2#{m}")
    u2 = create_user("otheruser2#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
	@log.info("As testuser#{m} inviting otheruser#{m} as a friend, colleque, follower")
    cm.invite_contact(u2.name, ["friend", "colleague"], ["follower"], ["leader"])
	@log.info("Checking relationship testuser#{m} invited otheruser#{m} as a friend, colleque, follower")
    check_contact_relationships(cm, "friend", "colleague", "follower")
    @s.switch_user(u2)
	@log.info("Checking relationship  otheruser#{m} was invited by testuser#{m} as a friend, colleque, leader")
    check_contact_relationships(cm, "friend", "colleague", "leader")
  end

  def test_removed_and_revised_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser3#{m}")
    u2 = create_user("otheruser3#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, [], ["teacher"], ["student"])
    @s.switch_user(u2)
    @log.info "About to accept invitation"
    cm.accept_contact(u1.name)
    @log.info "About to remove contact"
    cm.remove_contact(u1.name)
    @log.info "Afterwards..."
    assert_equal(0, cm.get_all()["results"].length, "Should have removed all contacts")
    @s.switch_user(u1)
    cm.invite_contact(u2.name, ["colleague"])
    @s.switch_user(u2)
    cm.accept_contact(u1.name)
    check_contact_relationships(cm, "colleague")
  end

  def check_contact_relationships(cm, *relationships)
    contact = cm.get_all()
    assert_not_nil(contact["results"])
    assert_not_nil(contact["results"][0]," Expected to have a contact  "+contact["results"].to_s())
    assert_not_nil(contact["results"][0]["details"])
    firstContact = contact["results"][0]["details"]
    types = firstContact["sakai:types"]
    assert_not_nil(types, "Expected relationships to be stored")
    assert_equal(relationships.length, types.length, "Should have #{relationships.length} relationships")
    relationships.each {
      |relationship| assert(types.include?(relationship), "Relationships should include '#{relationship}'")
    }
  end

end

