#!/usr/bin/ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/contacts'
include SlingInterface
include SlingContacts

class ContactCollisionTest < Test::Unit::TestCase
  include SlingTest

  def test_create_collision 
    users = (0..1).collect {|i| create_user("my_test_user" + i.to_s + Time.now.to_i.to_s)}
    others = {}
    users.each do |u|
      others[u.name] = []
      users.each do |u2|
        others[u.name] << u2.name if u2.name != u.name
      end
    end
    threads = []
    results = []
    for user in users
      threads << Thread.new(user) do |u|
        s = Sling.new
        cm = ContactManager.new(s)
        s.switch_user(u)
        others[u.name].each do |o|
          res = cm.invite_contact(o, "friend")
          while res.code.to_s == "409" do
            res = cm.invite_contact(o, "friend")
          end
          results << res
        end
      end
    end
    threads.each { |aThread|  aThread.join }
    results.each { |res| assert_equal("20", res.code.slice(0,2), "Expected invitation to succeed") }
  end

  def test_create_no_collision 
    users = (0..2).collect {|i| create_user(i.to_s + "my_test_user" + Time.now.to_i.to_s)}
    threads = []
    results = []
    trials = [ [ users[0], users[1] ],
               [ users[0], users[2] ],
               [ users[2], users[1] ] ]
    for trial in trials
      threads << Thread.new(trial) do |t|
        s = Sling.new
        #s.debug = true
        cm = ContactManager.new(s)
        s.switch_user(t[0])
        res = cm.invite_contact(t[1].name, "friend")
          while res.code.to_s == "409" do
            res = cm.invite_contact(t[1].name, "friend")
          end
        results << res
      end
    end
    threads.each { |aThread|  aThread.join }
    results.each { |res| assert_equal("20", res.code.slice(0,2), "Expected invitation to succeed") }
  end

end


