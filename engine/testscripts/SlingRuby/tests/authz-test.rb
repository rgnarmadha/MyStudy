#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'authz-base.rb'
include SlingInterface
include SlingUsers
include SlingAuthz

class TC_MyAuthZTest < Test::Unit::TestCase
  include AuthZBase


#
# This test creates a node at test/authztest/node* , sets a property on that node
# grants read and write to user1
# grants read to user2 denies write to user2
# grants read and write to group1
# grants read to group2 and denies write to group2 
# And then checks that the final ACL matches that.
# It does not test if user1 and user2 have permissions granted or denied.
#
  def test_authz
    m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	group1 = "g-group1-"+m
	group2 = "g-group2-"+m
	
	path = "test/authztest/node"+m
	create_user(user1)
	create_user(user2)
	create_group(group1)
	create_group(group2)
	@log.info("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	
        updateAcl(path,user1,true,true)
        updateAcl(path,user2,true,false)
        updateAcl(path,group1,true,true)
        updateAcl(path,group2,true,false)

        acl = @authz.getacl(path)
	

        checkAcl(path,user1,true,true)
        checkAcl(path,user2,true,false)
        checkAcl(path,group1,true,true)
        checkAcl(path,group2,true,false)

          
  end
  
  def test_NodeAuthZ
  
	m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	group1 = "g-group1-"+m
	group2 = "g-group2-"+m
	group3 = "g-group3-"+m
	user3 = "user3-"+m
	user4 = "user4-"+m
	user5 = "user5-"+m
	user6 = "user6-"+m
	user7 = "user7-"+m
	
	# add user3 to group1 and user4 to group2
	
	
	path = "test/authztest/node"+m
	u1 = create_user(user1)
	u2 = create_user(user2)
	u3 = create_user(user3)
	u4 = create_user(user4)
	u5 = create_user(user5)
	u6 = create_user(user6)
	u7 = create_user(user7)
	g1 = create_group(group1)
	g2 = create_group(group2)
	g3 = create_group(group3)
	# Add user3 to group 1 and user4 to group2
	g1.add_member(@s,user3,"user")
	g2.add_member(@s,user4,"user")
	g3.add_member(@s,user6,"user")
	
	# check that the users are members in the right way.ZZ
	assert_equal(true,g1.has_member(@s,user3))
	assert_equal(true,g2.has_member(@s,user4))
	assert_equal(true,g3.has_member(@s,user6))
	
		
	@log.info("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	
	# set all the acls
        # user1 can read and write
        # user2 can read
        # user5 cant
        # group1 can read and write ( ie user3)
        # group2 can read (ie user4)
        # group 3 cant (user6)
        updateAcl(path,user1,true,true)
	updateAcl(path,user2,true,false)
	updateAcl(path,user5,false,false)
	updateAcl(path,group1,true,true)
	updateAcl(path,group2,true,false)
	updateAcl(path,group3,false,false)

    # check the acls are set right on the node
	checkAcl(path,user1,true,true)
	checkAcl(path,user2,true,false)
	checkAcl(path,group1,true,true)
	checkAcl(path,group2,true,false)

        # If the test gets to here the ACLs are all stored correctly in the JCR, now we need to see if they will work.

	# check Http access (read, write)
	checkHttpAccess(path,u1,"",true,true)
	checkHttpAccess(path,u2,"",true,false)
	checkHttpAccess(path,u3," as a member of group 1",true,true)
	checkHttpAccess(path,u4," as a member of group 2",true,false)
	checkHttpAccess(path,u5,"",false,false)
	checkHttpAccess(path,u6," as a member og group 3",false,false)
	checkHttpAccess(path,u7," as a member of the root group everyone ",true,false)

  end

  def test_NodeAuthZChild
  
	m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	group1 = "g-group1-"+m
	group2 = "g-group2-"+m
	group3 = "g-group3-"+m
	user3 = "user3-"+m
	user4 = "user4-"+m
	user5 = "user5-"+m
	user6 = "user6-"+m
	user7 = "user7-"+m
	
	# add user3 to group1 and user4 to group2
	
	
	path = "test/authztest/node"+m
	u1 = create_user(user1)
	u2 = create_user(user2)
	u3 = create_user(user3)
	u4 = create_user(user4)
	u5 = create_user(user5)
	u6 = create_user(user6)
	u7 = create_user(user7)
	g1 = create_group(group1)
	g2 = create_group(group2)
	g3 = create_group(group3)
	# Add user3 to group 1 and user4 to group2
	g1.add_member(@s,user3,"user")
	g2.add_member(@s,user4,"user")
	g3.add_member(@s,user6,"user")
	
	# check that the users are members in the right way.ZZ
	assert_equal(true,g1.has_member(@s,user3))
	assert_equal(true,g2.has_member(@s,user4))
	assert_equal(true,g3.has_member(@s,user6))
	
		
	@log.info("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	childPath = path+"/childnode"
	create_node(path+"/childnode","testchildproperty" => "testvalue")
	
	# set all the acls
    updateAcl(path,user1,true,true)
	updateAcl(path,user2,true,false)
	updateAcl(path,user5,false,false)
	updateAcl(path,group1,true,true)
	updateAcl(path,group2,true,false)
	updateAcl(path,group3,false,false)

    # check the acls are set right on the node
	checkAcl(path,user1,true,true)
	checkAcl(path,user2,true,false)
	checkAcl(path,group1,true,true)
	checkAcl(path,group2,true,false)

	# check Http access (read, write)
	checkHttpAccess(childPath,u1,"",true,true)
	checkHttpAccess(childPath,u2,"",true,false)
	checkHttpAccess(childPath,u3," as a member of group 1",true,true)
	checkHttpAccess(childPath,u4," as a member of group 2",true,false)
	checkHttpAccess(childPath,u5,"",false,false)
	checkHttpAccess(childPath,u6," as a member og group 3",false,false)
	checkHttpAccess(childPath,u7," as a member of the root group everyone ",true,false)
	checkHttpAccess(childPath,SlingUsers::AnonymousUser.new,"",true,false)
	
	
  end
  
def test_NodeAuthZChildPrivate
  
	m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	group1 = "g-group1-"+m
	group2 = "g-group2-"+m
	group3 = "g-group3-"+m
	user3 = "user3-"+m
	user4 = "user4-"+m
	user5 = "user5-"+m
	user6 = "user6-"+m
	user7 = "user7-"+m
	
	# add user3 to group1 and user4 to group2
	
	
	path = "test/authztest/node"+m
	u1 = create_user(user1)
	u2 = create_user(user2)
	u3 = create_user(user3)
	u4 = create_user(user4)
	u5 = create_user(user5)
	u6 = create_user(user6)
	u7 = create_user(user7)
	g1 = create_group(group1)
	g2 = create_group(group2)
	g3 = create_group(group3)
	everyone = SlingUsers::Group.new("everyone")
	# Add user3 to group 1 and user4 to group2
	g1.add_member(@s,user3,"user")
	g2.add_member(@s,user4,"user")
	g3.add_member(@s,user6,"user")
	
	# check that the users are members in the right way.ZZ
	assert_equal(true,g1.has_member(@s,user3))
	assert_equal(true,g2.has_member(@s,user4))
	assert_equal(true,g3.has_member(@s,user6))
	
		
	@log.info("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	childPath = path+"/childnode"
	create_node(path+"/childnode","testchildproperty" => "testvalue")
	
	# set all the acls
    updateAcl(path,user1,true,true)
	updateAcl(path,user2,true,false)
	updateAcl(path,user5,false,false)
	updateAcl(path,group1,true,true)
	updateAcl(path,group2,true,false)
	updateAcl(path,group3,false,false)
	# deny everyone from the node
	updateAcl(path,"everyone",false,false)
	# but also explicity deny anon, since everyone is all authenticated users
	updateAcl(path,"anonymous",false,false)

    # check the acls are set right on the node
	checkAcl(path,user1,true,true)
	checkAcl(path,user2,true,false)
	checkAcl(path,group1,true,true)
	checkAcl(path,group2,true,false)
	checkAcl(path,group3,false,false)
	checkAcl(path,"everyone",false,false)
	checkAcl(path,"anonymous",false,false)

	# check Http access (read, write)
	checkHttpAccess(childPath,u1,"",true,true)
	checkHttpAccess(childPath,u2,"",true,false)
	checkHttpAccess(childPath,u3," as a member of group 1",true,true)
	checkHttpAccess(childPath,u4," as a member of group 2",true,false)
	checkHttpAccess(childPath,u5,"",false,false)
	checkHttpAccess(childPath,u6," as a member og group 3",false,false)
	checkHttpAccess(childPath,u7," as a member of the root group everyone ",false,false)
	
	
	@s.execute_get(@s.url_for("A_3MARKER_BEFORE"))
	checkHttpAccess(childPath,SlingUsers::AnonymousUser.new," authenticated users are allowed access, but anon users are not, this is a BUG somewhere ",false,false)
	@s.execute_get(@s.url_for("A_3MARKER_AFTER"))
	
	
  end

end


