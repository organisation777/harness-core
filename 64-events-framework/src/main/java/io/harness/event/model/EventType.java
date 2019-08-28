package io.harness.event.model;

public enum EventType {
  USER_INVITED_FROM_EXISTING_ACCOUNT,
  COMPLETE_USER_REGISTRATION,
  FIRST_DELEGATE_REGISTERED,
  FIRST_WORKFLOW_CREATED,
  FIRST_DEPLOYMENT_EXECUTED,
  FIRST_VERIFIED_DEPLOYMENT,
  FIRST_ROLLED_BACK_DEPLOYMENT,
  SETUP_CV_24X7,
  SETUP_2FA,
  SETUP_SSO,
  SETUP_IP_WHITELISTING,
  SETUP_RBAC,
  TRIAL_TO_PAID,
  TRIAL_TO_COMMUNITY,
  COMMUNITY_TO_PAID,
  COMMUNITY_TO_ESSENTIALS,
  ESSENTIALS_TO_PAID,
  PAID_TO_ESSENTIALS,
  TRIAL_TO_ESSENTIALS,
  /***
   * Usage metrics EventTypes
   */
  CV_META_DATA,

  OPEN_ALERT,

  NEW_TRIAL_SIGNUP,
  LICENSE_UPDATE,

  DEPLOYMENT_VERIFIED,
  JOIN_ACCOUNT_REQUEST,

  /**
   * Runtime entites
   */
  DEPLOYMENT_EVENT,
  INSTANCE_EVENT,

  CUSTOM,
  TECH_STACK,
  ACCOUNT_ENTITY_CHANGE,
  BLACKOUT_WINDOW_UPDATED,
  SECRET_MANAGER_TYPE;
}
