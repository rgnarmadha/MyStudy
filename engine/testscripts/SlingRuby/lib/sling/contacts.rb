#!/usr/bin/env ruby

module SlingContacts

  class ContactManager

    def initialize(sling)
      @sling = sling
    end

    def invite_contact(name, sharedRelationships, fromRelationships=[], toRelationships=[])
      case sharedRelationships
        when String
        sharedRelationships = [sharedRelationships]
      end
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.invite.html"), "sakai:types" => sharedRelationships,
        "fromRelationships" => fromRelationships, "toRelationships" => toRelationships, "targetUserId" => name)
    end
 
    def accept_contact(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.accept.html"), {"targetUserId" => name})
    end

    def reject_contact(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.reject.html"), {"targetUserId" => name})
    end

    def ignore_contact(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.ignore.html"), {"targetUserId" => name})
    end

    def block_contact(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.block.html"), {"targetUserId" => name})
    end

    def remove_contact(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.remove.html"), {"targetUserId" => name})
    end

    def cancel_invitation(name)
      home = @sling.get_user().home_path_for(@sling)
      return @sling.execute_post(@sling.url_for("#{home}/contacts.cancel.html"), {"targetUserId" => name})
    end


    def get_accepted()
			return find_contacts("ACCEPTED")
    end

    def get_pending()
			return find_contacts("PENDING")
		end

    def get_invited()
			return find_contacts("INVITED")
    end

    def get_blocked()
			return find_contacts("BLOCKED")
    end

    def get_ignored()
			return find_contacts("IGNORED")
    end

    def get_all()
			return find_contacts("*")
   end

		def find_contacts(state)
			result = @sling.execute_get(@sling.url_for("var/contacts/find?state=#{state}"))
    	return JSON.parse(result.body)
		end
    
  end

end
