package edu.utexas.tacc.tapis.systems.client;

/* Shared data and methods for running the systems client tests.
 *
 * Notes on environment required to run the tests:
 *  - Systems service base URL comes from the env or the default hard coded base URL.
 *  - Auth and tokens base URL comes from the env or the default hard coded base URL.
 *  - Auth service is used to get a short term JWT.
 *  - Tokens service is used to get a files service JWT.
 *  - Files service password must come from the environment: TAPIS_FILES_SVC_PASSWORD
 *
 * TAPIS_BASE_URL_SUFFIX should be set according to the dev, staging or prod environment
 *   dev     -> develop.tapis.io
 *   staging -> staging.tapis.io
 *   prod    -> tapis.io
 *
 *  To override base URLs use the following env variables:
 *    TAPIS_SVC_URL_SYSTEMS
 *    TAPIS_BASE_URL (for auth, tokens services)
 *  To override systems service port use:
 *    TAPIS_SERVICE_PORT
 *  NOTE that service port is ignored if TAPIS_SVC_URL_SYSTEMS is set
 */

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.client.shared.ClientTapisGsonUtils;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.systems.client.gen.model.Capability;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import edu.utexas.tacc.tapis.systems.client.gen.model.ReqCreateSystem;
import edu.utexas.tacc.tapis.systems.client.gen.model.ReqUpdateSystem;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Utilities and data for integration testing
 */
public final class Utils
{
  // Test data
  public static final String tenantName = "dev";
  public static final String testUser1 = "testuser1";
  public static final String testUser2 = "testuser2";
  public static final String testUser3 = "testuser3";
  public static final String testUser4 = "testuser4";
  public static final String testUser9 = "testuser9";
  public static final String adminUser = testUser9;
  public static final String ownerUser1 = testUser1;
  public static final String ownerUser2 = testUser2;
  public static final String masterTenantName = "master";
  public static final String filesSvcName = "files";
  public static final String sysType = ReqCreateSystem.SystemTypeEnum.LINUX.name();
  // TAPIS_BASE_URL_SUFFIX should be set according to the dev, staging or prod environment
  // dev     -> develop.tapis.io
  // staging -> staging.tapis.io
  // prod    -> tapis.io
  // Default URLs. These can be overridden by env variables
  public static final String DEFAULT_BASE_URL = "https://" + tenantName + ".develop.tapis.io";
  public static final String DEFAULT_BASE_URL_SYSTEMS = "http://localhost";
  public static final String DEFAULT_SVC_PORT = "8080";
  // Env variables for setting URLs, passwords, etc
  public static final String TAPIS_ENV_BASE_URL = "TAPIS_BASE_URL";
  public static final String TAPIS_ENV_SVC_URL_SYSTEMS = "TAPIS_SVC_URL_SYSTEMS";
  public static final String TAPIS_ENV_FILES_SVC_PASSWORD = "TAPIS_FILES_SERVICE_PASSWORD";
  public static final String TAPIS_ENV_SVC_PORT = "TAPIS_SERVICE_PORT";

  public static final int prot1Port = 22, prot1ProxyPort = 0, prot2Port = 0, prot2ProxyPort = 2222;
  public static final boolean prot1UseProxy = false, prot2UseProxy = true;
  public static final String prot1ProxyHost = "proxyhost1", prot2ProxyHost = "proxyhost2";

  public static final List<ReqCreateSystem.TransferMethodsEnum> prot1TxfrMethodsC =
          Arrays.asList(ReqCreateSystem.TransferMethodsEnum.SFTP, ReqCreateSystem.TransferMethodsEnum.S3);
  public static final List<TSystem.TransferMethodsEnum> prot1TxfrMethodsT =
          Arrays.asList(TSystem.TransferMethodsEnum.SFTP, TSystem.TransferMethodsEnum.S3);
  public static final List<ReqUpdateSystem.TransferMethodsEnum> prot2TxfrMethodsU =
          Collections.singletonList(ReqUpdateSystem.TransferMethodsEnum.SFTP);
  public static final List<TSystem.TransferMethodsEnum> prot2TxfrMethodsT =
          Collections.singletonList(TSystem.TransferMethodsEnum.SFTP);
  public static final SystemsClient.AccessMethod prot1AccessMethod = SystemsClient.AccessMethod.PKI_KEYS;
  public static final SystemsClient.AccessMethod prot2AccessMethod = SystemsClient.AccessMethod.ACCESS_KEY;

  public static final List<String> tags1 = Arrays.asList("value1", "value2", "a",
          "a long tag with spaces and numbers (1 3 2) and special characters [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped.");
  public static final List<String> tags2 = Arrays.asList("value3", "value4");
  public static final JsonObject notes1JO =
          ClientTapisGsonUtils.getGson().fromJson("{\"project\":\"myproj1\", \"testdata\":\"abc1\"}", JsonObject.class);
  public static final JsonObject notes2JO =
          ClientTapisGsonUtils.getGson().fromJson("{\"project\":\"myproj2\", \"testdata\":\"abc2\"}", JsonObject.class);
  public static final List<String> testPerms = new ArrayList<>(List.of("READ", "MODIFY"));

  private static final Capability capA1 = SystemsClient.buildCapability(Capability.CategoryEnum.SCHEDULER, "Type", "Slurm");
  private static final Capability capB1 = SystemsClient.buildCapability(Capability.CategoryEnum.HARDWARE, "CoresPerNode", "4");
  private static final Capability capC1 = SystemsClient.buildCapability(Capability.CategoryEnum.SOFTWARE, "OpenMP", "4.5");
  public static final List<Capability> jobCaps1 = new ArrayList<>(List.of(capA1, capB1, capC1));
  private static final Capability capA2 = SystemsClient.buildCapability(Capability.CategoryEnum.SCHEDULER, "Type", "Condor");
  private static final Capability capB2 = SystemsClient.buildCapability(Capability.CategoryEnum.HARDWARE, "CoresPerNode", "128");
  private static final Capability capC2 = SystemsClient.buildCapability(Capability.CategoryEnum.SOFTWARE, "OpenMP", "3.1");
  private static final Capability capD2 = SystemsClient.buildCapability(Capability.CategoryEnum.CONTAINER, "Singularity", null);
  public static final List<Capability> jobCaps2 = new ArrayList<>(List.of(capA2, capB2, capC2, capD2));

  public static final String sysNamePrefix = "CSys";

  // Strings for searches involving special characters
  public static final String specialChar7Str = ",()~*!\\"; // These 7 may need escaping
  public static final String specialChar7LikeSearchStr = "\\,\\(\\)\\~\\*\\!\\\\"; // All need escaping for LIKE/NLIKE
  public static final String specialChar7EqSearchStr = "\\,\\(\\)\\~*!\\"; // All but *! need escaping for other operators

  // String for search involving an escaped comma in a list of values
  public static final String escapedCommaInListValue = "abc\\,def";


  /**
   * Create an array of TSystem objects in memory
   * Names will be of format TestSys_K_NNN where K is the key and NNN runs from 000 to 999
   * We need a key because maven runs the tests in parallel so each set of systems created by an integration
   *   test will need its own namespace.
   * @param n number of systems to create
   * @return array of TSystem objects
   */
  public static Map<Integer, String[]> makeSystems(int n, String key)
  {
    Map<Integer, String[]> systems = new HashMap<>();
    for (int i = 1; i <= n; i++)
    {
      // Suffix which should be unique for each system within each integration test
      String suffix = key + "_" + String.format("%03d", i);
      String name = sysNamePrefix + "_" + suffix;
      // Constructor initializes all attributes except for JobCapabilities and Credential
      String[] sys0 = {tenantName, name, "description " + suffix, sysType, ownerUser1, "host"+suffix, "effUser"+suffix,
              "fakePassword"+suffix,"bucket"+suffix, "/root"+suffix, "jobLocalWorkDir"+suffix, "jobLocalArchDir"+suffix,
              "jobRemoteArchSystem"+suffix, "jobRemoteArchDir"+suffix};
      systems.put(i, sys0);
    }
    return systems;
  }

  public static String getFilesSvcPassword()
  {
    String s = System.getenv(TAPIS_ENV_FILES_SVC_PASSWORD);
    if (StringUtils.isBlank(s))
    {
      System.out.println("ERROR: Files service password must be set using environment variable:  " + TAPIS_ENV_FILES_SVC_PASSWORD);
      System.exit(1);
    }
    return s;
  }
  public static String getServicePort()
  {
    String s = System.getenv(TAPIS_ENV_SVC_PORT);
    System.out.println("Systems Service port from ENV: " + s);
    if (StringUtils.isBlank(s)) s = DEFAULT_SVC_PORT;
    return s;
  }
  public static String getServiceURL(String servicePort)
  {
    String s = System.getenv(TAPIS_ENV_SVC_URL_SYSTEMS);
    System.out.println("Systems Service URL from ENV: " + s);
    if (StringUtils.isBlank(s)) s = DEFAULT_BASE_URL_SYSTEMS + ":" + servicePort;
    return s;
  }
  public static String getBaseURL()
  {
    String s = System.getenv(TAPIS_ENV_BASE_URL);
    if (StringUtils.isBlank(s)) s = DEFAULT_BASE_URL;
    return s;
  }

  public static String createSystem(SystemsClient clt, String[] sys, int port, SystemsClient.AccessMethod accessMethod, Credential credential,
                             List<ReqCreateSystem.TransferMethodsEnum> txfrMethods)
          throws TapisClientException
  {
    var accMethod = accessMethod != null ? accessMethod : prot1AccessMethod;
    var tMethods = txfrMethods != null ? txfrMethods : prot1TxfrMethodsC;
    ReqCreateSystem rSys = new ReqCreateSystem();
    rSys.setName(sys[1]);
    rSys.description(sys[2]);
    rSys.setSystemType(ReqCreateSystem.SystemTypeEnum.valueOf(sys[3]));
    rSys.owner(sys[4]);
    rSys.setHost(sys[5]);
    rSys.enabled(true);
    rSys.effectiveUserId(sys[6]);
    rSys.defaultAccessMethod(ReqCreateSystem.DefaultAccessMethodEnum.valueOf(accMethod.name()));
    rSys.accessCredential(credential);
    rSys.bucketName(sys[8]);
    rSys.rootDir(sys[9]);
    rSys.setTransferMethods(tMethods);
//    rSys.port(prot1Port).useProxy(prot1UseProxy).proxyHost(prot1ProxyHost).proxyPort(prot1ProxyPort);
    rSys.port(port).useProxy(prot1UseProxy).proxyHost(prot1ProxyHost).proxyPort(prot1ProxyPort);
    rSys.jobCanExec(true);
    rSys.jobLocalWorkingDir(sys[10]).jobLocalArchiveDir(sys[11]).jobRemoteArchiveSystem(sys[12]).jobRemoteArchiveDir(sys[13]);
    rSys.jobCapabilities(jobCaps1);
    rSys.tags(tags1);
    rSys.notes(notes1JO);
    // Convert list of TransferMethod enums to list of strings
//    List<String> transferMethods = Stream.of(txfrMethodsStrList).map(TransferMethodsEnum::name).collect(Collectors.toList());
    // Create the system
    return clt.createSystem(rSys);
  }
  public static SystemsClient getClientUsr(String serviceURL, String userJWT)
  {
    // Create the client each time due to issue with setting different headers needed by svc vs usr client

    // Creating a separate client for svc is not working because headers end up being used for all clients.
    // Underlying defaultHeaderMap is static so adding headers impacts all clients.
//    sysClientSvc = new SystemsClient(systemsURL, svcJWT);
//    sysClientSvc.addDefaultHeader("X-Tapis-User", sysOwner);
//    sysClientSvc.addDefaultHeader("X-Tapis-Tenant", tenantName);
    return new SystemsClient(serviceURL, userJWT);
  }
}
