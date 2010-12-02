#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern485Test < Test::Unit::TestCase
  include SlingTest

  def checkGetAccess(path, user, expected)
    @s.switch_user(user)
    res = @s.execute_get(@s.url_for(path+".json"))
    assert_equal(expected, res.code, "Expected #{expected} for GET #{path} from" + user.to_s())  	
  end

  def test_site_creator_access
    m = Time.now.to_i.to_s
    @authz = SlingAuthz::Authz.new(@s)
    creatorid = "testuser_creator#{m}"
    creator = create_user(creatorid)
    siteid = "testsite#{m}"
    @s.switch_user(creator)
    site = @sm.create_site("sites", "Site test", "/#{siteid}")
    sitepath = "sites/#{siteid}"

    # Bring the site offline.
    everyone = SlingUsers::Group.new("everyone")
    @authz.grant(sitepath, "everyone", {"jcr:read" => "denied"})

    # Make sure the creator can still reach the site.
    checkGetAccess(sitepath, creator, "200");
  end

end

