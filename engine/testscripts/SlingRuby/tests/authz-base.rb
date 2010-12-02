#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingInterface
include SlingUsers

module AuthZBase
  include SlingTest

# This method assumes that the node, users and groups exist, read a write are denied for the denyUserread read and write are granted
# all are granted read
# 
  def updateAcl(path, principal, readGrant, writeGrant)
      @log.info("Updating #{path} for #{principal} with read #{readGrant} and write #{writeGrant} ")
      if ( readGrant ) then
         @authz.grant(path,principal,"jcr:read" => "granted")
      else
         @authz.grant(path,principal,"jcr:read" => "denied")
      end
      if ( writeGrant ) then
         @authz.grant(path,principal,"jcr:write" => "granted", "jcr:nodeTypeManagement" => "granted")
      else
         @authz.grant(path,principal,"jcr:write" => "denied")
      end
  end

  def deleteAcl(path, principal)
	@authz.delete(path,principal)
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
	  @log.info("ACE for user #{principal} was "+@authz.hashToString(ace)+": Granted Priv "+ace["granted"].to_s)
	end
	if ( !readGranted || !writeGranted ) then
          assert_not_nil(ace["denied"],"Expected ace for #{principal} to have denied something, denied was nil "+@authz.hashToString(acl))
          @log.info("ACE for user #{principal} was "+@authz.hashToString(ace)+": Denied Priv  "+ace["denied"].to_s)
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


end


