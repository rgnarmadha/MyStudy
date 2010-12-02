#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/contacts'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingContacts

class TC_MyContactTest < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @cm = ContactManager.new(@s)
  end

  def test_connect_users
    m = Time.now.to_i.to_s
    @log.info("Creating user aaron"+m)
    a = create_user("aaron"+m)
    @log.info("Creating user nico"+m)
    n = create_user("nico"+m)
    @log.info("Creating user ian"+m)
    i = create_user("ian"+m)
    @s.switch_user(a)
    @log.info("Aaron Adding Nico as a coworker and friend")
    res = @cm.invite_contact("nico"+m, [ "coworker", "friend" ])
    assert_equal("200", res.code, "Expected to be able to request contact addition "+res.body)
    @log.info("Checking that The invitation to Nico is pending")
    contacts = @cm.get_pending()
    assert_not_nil(contacts, "Expected to get contacts back")
    assert_equal(1, contacts["results"].size, "Expected single pending request back")
    contact = contacts["results"][0]
    assert_equal("nico"+m, contact["target"], "Expected nico to be my friend")
    assert_equal("PENDING", contact["details"]["sakai:state"], "Expected state to be 'PENDING'")
    @log.info("Invitation from Aaron to Nico is present ")
   

    @s.switch_user(n)
    @log.info("Operating as Nico")
    contacts = @cm.get_invited()
    assert_not_nil(contacts, "Expected to get an invite back ")
    assert_equal(1, contacts["results"].size, "Only expecting a single invite for Nico ")
    contact = contacts["results"][0]
    assert_equal("aaron"+m,contact["target"], "Expected Aaron to be asking ")
    assert_equal("INVITED", contact["details"]["sakai:state"], "Expected state to be 'INVITED'") 
    @log.info("Nico is accpting invitation from Aaron")
    res = @cm.accept_contact("aaron"+m)
    assert_equal("200", res.code, "Expecting acceptance of the contact")
    contacts = @cm.get_accepted()
    assert_not_nil(contacts, "Expected to get an accepted back ")
    assert_equal(1, contacts["results"].size, "Only expecting a single acceptance ")
    contact = contacts["results"][0]
    assert_equal("aaron"+m,contact["target"], "Expected Nico to have been accepted ")
    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 

    @s.switch_user(a)
    @log.info("Operating as Aaron")
    contacts = @cm.get_accepted()
    assert_not_nil(contacts, "Expected to get an accepted back ")
    assert_equal(1, contacts["results"].size, "Only expecting a single acceptance ")
    contact = contacts["results"][0]
    assert_equal("nico"+m,contact["target"], "Expected Aaron to have been accepted ")
    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 
 

  end

  def teardown
    @created_users.each do |user|
      @s.switch_user(user)
      contacts = @cm.get_all()
      contacts["results"].each do |result|
        assert_not_nil(result["target"], "Expected contacts to have names")
        res = @cm.remove_contact(result["target"])
        assert_equal("200", res.code, "Expected removal to succeed")
      end
    end
    super
  end

end


