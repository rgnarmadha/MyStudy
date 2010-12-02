package org.sakaiproject.nakamura.api.docproxy;

import java.util.List;
import java.util.Map;

public interface XExternalRepository {
	
	/**
	 * Create a new Xythos directory for the specified user in the specified location
	 * @param username
	 * @param virtualServerName
	 * @param homeDirectory
	 * @param directoryName
	 */
	void createDirectory (String username, String virtualServerName, 
			String homeDirectory, String directoryName);
	
	Map<String, Object> getDocument(String path, String userId);
	
	byte[] getFileContent(String path, String userId);
	
	List<Map<String, Object>> doSearch(Map<String, Object> searchProperties, String userId);
	
	void updateFile(String path, byte[] fileData, Map<String, Object>properties, String userId);
	
	/**
	 * Either adds or removes the specified member from the specified group.
	 * If they're in the group, remove them
	 * If they're not in the group, add them
	 * 
	 * @param groupId
	 * @param userId
	 */
	void toggleMember(String groupId, String userId);
	
	
	void createGroup(String groupId, String userId);
	
	void removeDocument(String path, String userId);
	
	/**
	 * Request to share the specified file path, e.g. /zach/party.png, with the specified group, e.g. partytime
	 * 
	 * @param groupId
	 * @param filePath
	 * @param userId
	 * @return whether or not the share was a success 
	 */
	boolean shareFileWithGroup(String groupId, String filePath, String userId);
	


}
