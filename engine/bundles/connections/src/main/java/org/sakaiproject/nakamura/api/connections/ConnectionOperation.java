package org.sakaiproject.nakamura.api.connections;

/**
 * Indicates the operations which are valid for dealing with connections.
 * DO NOT! rename to upper case since we do a valueOf(selector) to generate the operation.
 */
public enum ConnectionOperation {
  /**
   * Invite someone to connect.
   */
  invite(), 
  /**
   * accept an invitation.
   */
  accept(), 
  /**
   * reject.
   */
  reject(), 
  /**
   * ignore.
   */
  ignore(), 
  /**
   * block.
   */
  block(), 
  /**
   * cancel an invitation.
   */
  cancel(), 
  /**
   * remove a connection.
   */
  remove(), 
  /**
   * dont do anything.
   */
  noop();
}
