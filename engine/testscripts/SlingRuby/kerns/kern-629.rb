#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
require '../tests/authz-base.rb'
include SlingSearch
include SlingUsers
include SlingContacts


class TC_Kern629Test < Test::Unit::TestCase
  include SlingTest, AuthZBase
  
  # Test Time Based ACLs
  def test_create_timebase_acl_rule
    user = createUser("1")
    node = createNode("1")
    res = @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"rule" => "TestingThatRuleWorks"})
	assert_equal("200",res.code,"Failed to add Rule ACL "+res.body)	
	
	acl = @s.get_node_ruleacl(node)
	@s.log.info(acl)
	assert_equal(1,acl.size)
	
	ruleace = findRuleAce(acl, user.name)
	assert_not_nil(ruleace)
	assert_equal(0,ruleace["order"])
	granted = ruleace["granted"]
	assert_equal(1,granted.size)
	assert_equal("jcr:read",granted[0])
	denied = ruleace["denied"]
	assert_nil(denied)
	assert_equal("TestingThatRuleWorks",ruleace["sakai:rule-processor"])
	
	
	
	
  end
  
  def findRuleAce(acl, name)
	acl.each do |k,v|
	    if ( k.index("sakai-rules:"+name) == 0 && k.length > ("sakai-rules:"+name).length )
		    return v
		end
	end
	return nil
  end
  
  def test_activate_timebase_acl_active
    user = createUser("2")
    node = createNode("2")
    res = @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"active" => ["20100304/20100404","2010-04-05T01:00:00Z/20100405T020000Z"]})
	assert_equal("200",res.code,"Failed to add Rule ACL "+res.body)
	
	
	acl = @s.get_node_ruleacl(node)
	@s.log.info(acl)
	assert_equal(1,acl.size)
	
	ruleace = findRuleAce(acl, user.name)
	assert_not_nil(ruleace)
	assert_equal(0,ruleace["order"])
	granted = ruleace["granted"]
	assert_equal(1,granted.size)
	assert_equal("jcr:read",granted[0])
	denied = ruleace["denied"]
	assert_nil(denied)
	active = ruleace["active"]
	assert_not_nil(active)
	assert_equal(2, active.size())
	assert_equal("2010-03-04/2010-04-04", active[0])
	assert_equal("2010-04-05T01:00:00Z/2010-04-05T02:00:00Z", active[1])
	inactive = ruleace["inactive"]
	assert_nil(inactive)

  end
  
  def test_deactivate_timebase_acl
    user = createUser("3")
    node = createNode("3")
    res = @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"inactive" => ["20100304/20100404","20100405T010000Z/20100405T020000Z"]})
	assert_equal("200",res.code,"Failed to add Rule ACL "+res.body)
	acl = @s.get_node_ruleacl(node)
	@s.log.info(acl)
	assert_equal(1,acl.size)
	
	ruleace = findRuleAce(acl, user.name)
	assert_not_nil(ruleace)
	assert_equal(0,ruleace["order"])
	granted = ruleace["granted"]
	assert_equal(1,granted.size)
	assert_equal("jcr:read",granted[0])
	denied = ruleace["denied"]
	assert_nil(denied)
	active = ruleace["inactive"]
	assert_not_nil(active)
	assert_equal(2, active.size())
	assert_equal("2010-03-04/2010-04-04", active[0])
	assert_equal("2010-04-05T01:00:00Z/2010-04-05T02:00:00Z", active[1])
	inactive = ruleace["active"]
	assert_nil(inactive)
	
  end
  
  def createUser(n) 
    m = Time.now.to_i.to_s
    return create_user("kern-629-user-#{n}-#{m}")
  end
  
  def createNode(n) 
    m = Time.now.to_i.to_s
    path = "/kern-629/testNode-#{n}-#{m}"
    @s.create_node(path , {"foo" => "bar"})
    return path
  end
  
  
end

