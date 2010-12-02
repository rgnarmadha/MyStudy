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

class Kern356Test < Test::Unit::TestCase
  include SlingTest

# This method assumes that the node, users and groups exist, read a write are denied for the denyUserread read and write are granted
# all are granted read
# 
  def updateAcl(path, principal, readGrant, writeGrant)
      if ( readGrant ) then
         @authz.grant(path,principal,"jcr:read" => "granted")
      else
         @authz.grant(path,principal,"jcr:read" => "denied")
      end
      if ( writeGrant ) then
         @authz.grant(path,principal,"jcr:write" => "granted")
      else
         @authz.grant(path,principal,"jcr:write" => "denied")
      end
  end

# This functon checks that the permissions are correct on a node
# It makes the assumption that the permissions are expressed on the node and not on a parent node.

  def checkAcl(path,principal,readGranted,writeGranted)
       acl = @authz.getacl(path)
	# check user1
	assert_not_nil(acl[principal],"Expected for find ACE for #{principal}"+@authz.hashToString(acl))
	ace = acl[principal]
	if ( readGranted || writeGranted ) then
	  assert_not_nil(ace["granted"],"Expected ace for #{principal} to have granted something granted ace was nil "+@authz.hashToString(acl))
	  @log.info("ACE for user #{principal} was "+@authz.hashToString(ace)+":"+ace["granted"].to_s)
	end
	if ( !readGranted || !writeGranted ) then
      assert_not_nil(ace["denied"],"Expected ace for #{principal} to have denied something, denied was nil "+@authz.hashToString(acl))
      @log.info("ACE for user #{principal} was "+@authz.hashToString(ace)+":"+ace["denied"].to_s)
     end

        if ( readGranted ) then
          assert_equal(true,ace["granted"].include?("jcr:read"),"Expected ace for #{principal} to have jcr:read granted ace was "+@authz.hashToString(ace))
          if ( ace["denied"] != nil ) then
             assert_equal(false,ace["denied"].include?("jcr:read"),"Expected ace for #{principal} not to have jcr:read denied ace was "+@authz.hashToString(ace))
	  end
        else
          assert_equal(true,ace["denied"].include?("jcr:read"),"Expected ace for #{principal} to have jcr:read denied ace was "+@authz.hashToString(ace))
          if ( ace["granted"] != nil ) then
             assert_equal(false,ace["granted"].include?("jcr:read"),"Expected ace for #{principal} not to have jcr:read granted ace was "+@authz.hashToString(ace))
	  end
        end
        if ( writeGranted ) then
          assert_equal(true,ace["granted"].include?("jcr:write"),"Expected ace for #{principal} to have jcr:write granted ace was "+@authz.hashToString(ace))
          if ( ace["denied"] != nil ) then
             assert_equal(false,ace["denied"].include?("jcr:write"),"Expected ace for #{principal} not to have jcr:write denied ace was "+@authz.hashToString(ace))
	  end
        else
          assert_equal(true,ace["denied"].include?("jcr:write"),"Expected ace for #{principal} to have jcr:write denied ace was "+@authz.hashToString(ace))
          if ( ace["granted"] != nil ) then
             assert_equal(false,ace["granted"].include?("jcr:write"),"Expected ace for #{principal} not to have jcr:write granted ace was "+@authz.hashToString(ace))
	  end
        end
  end
  
  # check http access on the path
  def checkHttpAccess(path, user, because, canRead, canWrite)
	@s.switch_user(user)
	res = @s.execute_get(@s.url_for(path+".json"))
	if ( canRead ) then 
	    assert_equal("200",res.code,"Should have been able to read the child node as "+user.to_s()+because)
	else 
	  assert_equal("404",res.code," Expected to get read denied for "+user.to_s()+because)	
	end
	res = @s.execute_post(@s.url_for(path+".html"),user.name => "testset")
	if ( canWrite ) then
		if ( res.code != "200" ) then
			@log.debug(res.body)
		end 
		assert_equal("200",res.code,"Should have been able to write to the node as "+user.to_s()+because)
	else
		assert_equal("500",res.code," Expected to get write denied for "+user.to_s()+because)
		assert_equal(true,res.body.include?("AccessDeniedException"), " Error was not an access denied exception for "+user.to_s()+because)
	end
  end

  
  def test_NodeAuthZChildPrivate
  
	m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	
	path = "test/authztest/node"+m
		
	@log.info("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	childPath = path+"/childnode"
	create_node(path+"/childnode","testchildproperty" => "testvalue")
	
	# but also explicity deny anon, since everyone is all authenticated users
	updateAcl(path,"anonymous",false,false)

	checkAcl(path,"anonymous",false,false)

	checkHttpAccess(childPath,SlingUsers::AnonymousUser.new," authenticated users are allowed access, but anon users are not, this is a BUG somewhere ",false,false)
	
	
  end
  
  def test_GetAllProfiles
	m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	u1 = create_user(user1)
	u2 = create_user(user2)
  	@s.switch_user(u1)
	res = @s.execute_post(@s.url_for("#{u1.private_path_for(@s)}/GetAllProfilesTest"+m+".html"),"testprop" => "testset")
	assert_equal("201",res.code,"Expected to be able to Create a private node "+res.body)
	res = @s.execute_get(@s.url_for("_user.tidy.infinity.json"))
	assert_equal("403",res.code,"Should not be able to list users"+res.body)
	
	
	
  	@s.switch_user(SlingUsers::AnonymousUser.new)
	res = @s.execute_get(@s.url_for("_user.infinity.json"))
	assert_equal("403",res.code,"Should not be able to list users"+res.body)

  	@s.switch_user(u2)
	res = @s.execute_get(@s.url_for("_user.infinity.json"))
	assert_equal("403",res.code,"Should not be able to list users"+res.body)
  end


end


