package edu.utexas.tacc.tapis.systems.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.systems.client.gen.model.ReqCreateCredential;
import edu.utexas.tacc.tapis.systems.client.gen.model.ReqUpdateSystem;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.client.shared.ClientTapisGsonUtils;
import edu.utexas.tacc.tapis.systems.client.gen.model.Capability;
import edu.utexas.tacc.tapis.systems.client.gen.model.Credential;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;
import edu.utexas.tacc.tapis.systems.client.SystemsClient.AccessMethod;
import edu.utexas.tacc.tapis.auth.client.AuthClient;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;

import static edu.utexas.tacc.tapis.systems.client.Utils.*;

/**
 * Test the Systems API client acting as a user calling the systems service.
 * Tests that retrieve credentials act as a files service client calling the systems service.
 *
 * See IntegrationUtils in this package for information on environment required to run the tests.
 * 
 */
@Test(groups={"integration"})
public class UserTest
{
  // Test data
  int numSystems = 16;
  Map<Integer, String[]> systems = Utils.makeSystems(numSystems, "CltUsr");
  
  private static final String newOwnerUser = testUser3;
  private static final String newPermsUser = testUser4;

  private String serviceURL, ownerUserJWT, newOwnerUserJWT, filesServiceJWT;

  @BeforeSuite
  public void setUp() throws Exception {
    // Get the base URLs from the environment so the test can be used in environments other than dev
    System.out.println("****** Executing BeforeSuite setup method for class: " + this.getClass().getSimpleName());
    // Get files service password from env
    String filesSvcPasswd = Utils.getFilesSvcPassword();
    // Set service port for systems service. Check for port set as env var
    // NOTE: This is ignored if TAPIS_ENV_SVC_URL_SYSTEMS is set
    String servicePort = Utils.getServicePort();
    // Set base URL for systems service. Check for URL set as env var
    serviceURL = Utils.getServiceURL(servicePort);
    // Get base URL suffix from env or from default
    String baseURL = Utils.getBaseURL();
    // Log URLs being used
    System.out.println("Using Systems URL: " + serviceURL);
    System.out.println("Using Authenticator URL: " + baseURL);
    System.out.println("Using Tokens URL: " + baseURL);
    // Get short term user JWT from tokens service
    var authClient = new AuthClient(baseURL);
    var tokClient = new TokensClient(baseURL, filesSvcName, filesSvcPasswd);
    try {
      ownerUserJWT = authClient.getToken(ownerUser1, ownerUser1);
      newOwnerUserJWT = authClient.getToken(newOwnerUser, newOwnerUser);
      filesServiceJWT = tokClient.getSvcToken(masterTenantName, filesSvcName);
    } catch (Exception e) {
      throw new Exception("Exception while creating tokens or auth service", e);
    }
    // Basic check of JWTs
    if (StringUtils.isBlank(ownerUserJWT)) throw new Exception("Authn service returned invalid owner user JWT");
    if (StringUtils.isBlank(newOwnerUserJWT)) throw new Exception("Authn service returned invalid new owner user JWT");
    if (StringUtils.isBlank(filesServiceJWT)) throw new Exception("Tokens service returned invalid files svc JWT");
    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @Test
  public void testHealthAndReady() {
    try {
      System.out.println("Checking health status");
      String status = getClientUsr(serviceURL, ownerUserJWT).checkHealth();
      System.out.println("Health status: " + status);
      Assert.assertNotNull(status);
      Assert.assertFalse(StringUtils.isBlank(status), "Invalid response: " + status);
      Assert.assertEquals(status, "success", "Service failed health check");
      System.out.println("Checking ready status");
      status = getClientUsr(serviceURL, ownerUserJWT).checkReady();
      System.out.println("Ready status: " + status);
      Assert.assertNotNull(status);
      Assert.assertFalse(StringUtils.isBlank(status), "Invalid response: " + status);
      Assert.assertEquals(status, "success", "Service failed ready check");
    } catch (Exception e) {
      System.out.println("Caught exception: " + e);
      Assert.fail();
    }
  }

  @Test
  public void testCreateSystem() {
    // Create a system
    String[] sys0 = systems.get(1);
    Credential cred0 = null;
    System.out.println("Creating system with name: " + sys0[1]);
    try {
      String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
      System.out.println("Created system: " + respUrl);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    } catch (Exception e) {
      System.out.println("Caught exception: " + e);
      Assert.fail();
    }
  }

  // Create a system using minimal attributes:
  //   name, systemType, host, defaultAccessMethod, jobCanExec
  @Test
  public void testCreateSystemMinimal()
  {
    // Create a system
    String[] sys0 = systems.get(14);
    System.out.println("Creating system with name: " + sys0[1]);
    // Set optional attributes to null
//    private static final String[] sysE = {tenantName, "CsysE", null, sysType, null, "hostE", null, null,
//            null, null, null, null, null, null};
    sys0[2] = null; sys0[4] = null; sys0[6] = null; sys0[7] = null; sys0[9] = null;
    sys0[10] = null; sys0[11] = null; sys0[12] = null; sys0[13] = null;

    try {
      String respUrl = createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, null, null);
      System.out.println("Created system: " + respUrl);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    } catch (Exception e) {
      System.out.println("Caught exception: " + e);
      Assert.fail();
    }
  }

  @Test(expectedExceptions = {TapisClientException.class}, expectedExceptionsMessageRegExp = "^SYSAPI_SYS_EXISTS.*")
  public void testCreateSystemAlreadyExists() throws Exception {
    // Create a system
    String[] sys0 = systems.get(7);
    Credential cred0 = null;
    System.out.println("Creating system with name: " + sys0[1]);
    try {
      String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
      System.out.println("Created system: " + respUrl);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    // Now attempt to create it again, should throw exception
    System.out.println("Creating system with name: " + sys0[1]);
    Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.fail("Exception should have been thrown");
  }

  // Test that bucketName is required if transfer methods include S3
  @Test(expectedExceptions = {TapisClientException.class}, expectedExceptionsMessageRegExp = ".*SYSAPI_S3_NOBUCKET_INPUT.*")
  public void testCreateSystemS3NoBucketName() throws Exception {
    // Create a system
    String[] sys0 = systems.get(8);
    // Set bucketName to empty string
    sys0[8] = "";
    Credential cred0 = null;
    System.out.println("Creating system with name: " + sys0[1]);
    Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.fail("Exception should have been thrown");
  }

  // Test that access method of CERT and static owner is not allowed
  @Test(expectedExceptions = {TapisClientException.class}, expectedExceptionsMessageRegExp = ".*SYSAPI_INVALID_EFFECTIVEUSERID_INPUT.*")
  public void testCreateSystemInvalidEffUserId() throws Exception {
    // Create a system
    String[] sys0 = systems.get(9);
    Credential cred0 = null;
    System.out.println("Creating system with name: " + sys0[1]);
    Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, AccessMethod.CERT, cred0, prot1TxfrMethodsC);
    Assert.fail("Exception should have been thrown");
  }

  // Test that providing credentials for dynamic effective user is not allowed
  @Test(expectedExceptions = {TapisClientException.class}, expectedExceptionsMessageRegExp = ".*SYSAPI_CRED_DISALLOWED_INPUT.*")
  public void testCreateSystemCredDisallowed() throws Exception {
    // Create a system
    String[] sys0 = systems.get(11);
    // Set effectiveUserId to api user
    sys0[6] = "${apiUserId}";
    Credential cred0 = SystemsClient.buildCredential(sys0[7], "fakePrivateKey", "fakePublicKey",
                                           "fakeAccessKey", "fakeAccessSecret", "fakeCert");
    System.out.println("Creating system with name: " + sys0[1]);
    Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.fail("Exception should have been thrown");
  }

  // Test retrieving a system including default access method
  //   and test retrieving for specified access method.
  // NOTE: Credential is created for effectiveUserId
  @Test
  public void testGetSystemByName() throws Exception {
    String[] sys0 = systems.get(2);
    Credential cred0 = SystemsClient.buildCredential(sys0[7], "fakePrivateKey", "fakePublicKey",
                                           "fakeAccessKey", "fakeAccessSecret", "fakeCert");
    String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, AccessMethod.PKI_KEYS, cred0, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);

    // Must be files or jobs service to get credentials
    TSystem tmpSys = getClientFilesSvc().getSystemByName(sys0[1], null);
    Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0[1]);
    System.out.println("Found item: " + sys0[1]);
    Assert.assertEquals(tmpSys.getName(), sys0[1]);
    Assert.assertEquals(tmpSys.getDescription(), sys0[2]);
    Assert.assertEquals(tmpSys.getSystemType().name(), sys0[3]);
    Assert.assertEquals(tmpSys.getOwner(), sys0[4]);
    Assert.assertEquals(tmpSys.getHost(), sys0[5]);
    Assert.assertEquals(tmpSys.getEffectiveUserId(), sys0[6]);
    Assert.assertEquals(tmpSys.getBucketName(), sys0[8]);
    Assert.assertEquals(tmpSys.getRootDir(), sys0[9]);
    Assert.assertEquals(tmpSys.getJobLocalWorkingDir(), sys0[10]);
    Assert.assertEquals(tmpSys.getJobLocalArchiveDir(), sys0[11]);
    Assert.assertEquals(tmpSys.getJobRemoteArchiveSystem(), sys0[12]);
    Assert.assertEquals(tmpSys.getJobRemoteArchiveDir(), sys0[13]);
    Assert.assertEquals(tmpSys.getPort().intValue(), prot1Port);
    Assert.assertEquals(tmpSys.getUseProxy().booleanValue(), prot1UseProxy);
    Assert.assertEquals(tmpSys.getProxyHost(), prot1ProxyHost);
    Assert.assertEquals(tmpSys.getProxyPort().intValue(), prot1ProxyPort);
    Assert.assertEquals(tmpSys.getDefaultAccessMethod().name(), prot1AccessMethod.name());
    // Verify credentials. Only cred for default accessMethod is returned. In this case PKI_KEYS.
    Credential cred = tmpSys.getAccessCredential();
    Assert.assertNotNull(cred, "AccessCredential should not be null");
// TODO: Getting cred along with system is currently broken when called from client.
// TODO Does work in systems service integration test. Parameters to SK appear to be the same so not clear why it fails here
// TODO: Figure out why this works using getUserCred and when called directly from svc but not when getting system using client  
// Cred retrieved should be for effectiveUserId = effUser2, so far now as a test retrieve cred directly which does work
//    cred = getClientFilesSvc().getUserCredential(sys0[1], sys0[6], AccessMethod.PKI_KEYS);
    Assert.assertEquals(cred.getPrivateKey(), cred0.getPrivateKey());
    Assert.assertEquals(cred.getPublicKey(), cred0.getPublicKey());
    Assert.assertNull(cred.getPassword(), "AccessCredential password should be null");
    Assert.assertNull(cred.getAccessKey(), "AccessCredential access key should be null");
    Assert.assertNull(cred.getAccessSecret(), "AccessCredential access secret should be null");
    Assert.assertNull(cred.getCertificate(), "AccessCredential certificate should be null");
    // Verify transfer methods
    List<TSystem.TransferMethodsEnum> tMethodsList = tmpSys.getTransferMethods();
    Assert.assertNotNull(tMethodsList, "TransferMethods list should not be null");
    for (TSystem.TransferMethodsEnum txfrMethod : prot1TxfrMethodsT)
    {
      Assert.assertTrue(tMethodsList.contains(txfrMethod), "List of transfer methods did not contain: " + txfrMethod.name());
    }
    // Verify capabilities
    List<Capability> jobCaps = tmpSys.getJobCapabilities();
    Assert.assertNotNull(jobCaps);
    Assert.assertEquals(jobCaps.size(), jobCaps1.size());
    var capNamesFound = new ArrayList<String>();
    for (Capability capFound : jobCaps) {capNamesFound.add(capFound.getName());}
    for (Capability capSeed : jobCaps1)
    {
      Assert.assertTrue(capNamesFound.contains(capSeed.getName()), "List of capabilities did not contain a capability named: " + capSeed.getName());
    }
    // Verify tags
    List<String> tmpTags = tmpSys.getTags();
    Assert.assertNotNull(tmpTags, "Tags value was null");
    Assert.assertEquals(tmpTags.size(), tags1.size(), "Wrong number of tags");
    for (String tagStr : tags1)
    {
      Assert.assertTrue(tmpTags.contains(tagStr));
      System.out.println("Found tag: " + tagStr);
    }
    // Verify notes
    String tmpNotesStr = (String) tmpSys.getNotes();
    System.out.println("Found notes: " + tmpNotesStr);
    JsonObject tmpNotes = ClientTapisGsonUtils.getGson().fromJson(tmpNotesStr, JsonObject.class);
    Assert.assertNotNull(tmpNotes, "Fetched Notes should not be null");
    JsonObject origNotes = notes1JO;
    Assert.assertTrue(tmpNotes.has("project"));
    String projStr = origNotes.get("project").getAsString();
    Assert.assertEquals(tmpNotes.get("project").getAsString(), projStr);
    Assert.assertTrue(tmpNotes.has("testdata"));
    String testdataStr = origNotes.get("testdata").getAsString();
    Assert.assertEquals(tmpNotes.get("testdata").getAsString(), testdataStr);

    // Need service client to get creds. Currently unable to use both user client and service client in same program
    // Test retrieval using specified access method
    tmpSys = getClientFilesSvc().getSystemByName(sys0[1], AccessMethod.PASSWORD);
    // Verify credentials. Only cred for default accessMethod is returned. In this case PASSWORD.
    cred = tmpSys.getAccessCredential();
    Assert.assertNotNull(cred, "AccessCredential should not be null");
// TODO Not working as described above. For now test by getting cred directly
// TODO fix it
    cred = getClientFilesSvc().getUserCredential(sys0[1], sys0[6], AccessMethod.PASSWORD);

    Assert.assertEquals(cred.getPassword(), cred0.getPassword());
    Assert.assertNull(cred.getPrivateKey(), "AccessCredential private key should be null");
    Assert.assertNull(cred.getPublicKey(), "AccessCredential public key should be null");
    Assert.assertNull(cred.getAccessKey(), "AccessCredential access key should be null");
    Assert.assertNull(cred.getAccessSecret(), "AccessCredential access secret should be null");
    Assert.assertNull(cred.getCertificate(), "AccessCredential certificate should be null");
  }

  @Test
  public void testUpdateSystem() {
    String[] sys0 = systems.get(15);
    Credential cred0 = null;
//    private static final String[] sysF2 = {tenantName, "CsysF", "description PATCHED", sysType, ownerUser, "hostPATCHED", "effUserPATCHED",
//            "fakePasswordF", "bucketF", "/rootF", "jobLocalWorkDirF", "jobLocalArchDirF", "jobRemoteArchSystemF", "jobRemoteArchDirF"};
    String[] sysF2 = sys0.clone();
    sysF2[2] = "description PATCHED"; sysF2[5] = "hostPATCHED"; sysF2[6] = "effUserPATCHED";
    ReqUpdateSystem rSystem = createPatchSystem(sysF2);
    System.out.println("Creating and updating system with name: " + sys0[1]);
    try {
      // Create a system
      String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
      System.out.println("Created system: " + respUrl);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
      // Update the system
      respUrl = getClientUsr(serviceURL, ownerUserJWT).updateSystem(sys0[1], rSystem);
      System.out.println("Updated system: " + respUrl);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
      // Verify attributes
      sys0 = sysF2;
      TSystem tmpSys = getClientUsr(serviceURL, ownerUserJWT).getSystemByName(sys0[1]);
      Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0[1]);
      System.out.println("Found item: " + sys0[1]);
      Assert.assertEquals(tmpSys.getName(), sys0[1]);
      Assert.assertEquals(tmpSys.getDescription(), sys0[2]);
      Assert.assertEquals(tmpSys.getSystemType().name(), sys0[3]);
      Assert.assertEquals(tmpSys.getOwner(), sys0[4]);
      Assert.assertEquals(tmpSys.getHost(), sys0[5]);
      Assert.assertEquals(tmpSys.getEffectiveUserId(), sys0[6]);
      Assert.assertEquals(tmpSys.getBucketName(), sys0[8]);
      Assert.assertEquals(tmpSys.getRootDir(), sys0[9]);
      Assert.assertEquals(tmpSys.getJobLocalWorkingDir(), sys0[10]);
      Assert.assertEquals(tmpSys.getJobLocalArchiveDir(), sys0[11]);
      Assert.assertEquals(tmpSys.getJobRemoteArchiveSystem(), sys0[12]);
      Assert.assertEquals(tmpSys.getJobRemoteArchiveDir(), sys0[13]);
      Assert.assertEquals(tmpSys.getPort().intValue(), prot2Port);
      Assert.assertEquals(tmpSys.getUseProxy().booleanValue(), prot2UseProxy);
      Assert.assertEquals(tmpSys.getProxyHost(), prot2ProxyHost);
      Assert.assertEquals(tmpSys.getProxyPort().intValue(), prot2ProxyPort);
      Assert.assertEquals(tmpSys.getDefaultAccessMethod().name(), prot2AccessMethod.name());
      // Verify transfer methods
      List<TSystem.TransferMethodsEnum> tMethodsList = tmpSys.getTransferMethods();
      Assert.assertNotNull(tMethodsList, "TransferMethods list should not be null");
      for (TSystem.TransferMethodsEnum txfrMethod : prot2TxfrMethodsT)
      {
        Assert.assertTrue(tMethodsList.contains(txfrMethod), "List of transfer methods did not contain: " + txfrMethod.name());
      }
      // Verify capabilities
      List<Capability> jobCaps = tmpSys.getJobCapabilities();
      Assert.assertNotNull(jobCaps);
      Assert.assertEquals(jobCaps.size(), jobCaps2.size());
      var capNamesFound = new ArrayList<String>();
      for (Capability capFound : jobCaps) {capNamesFound.add(capFound.getName());}
      for (Capability capSeed : jobCaps2)
      {
        Assert.assertTrue(capNamesFound.contains(capSeed.getName()), "List of capabilities did not contain a capability named: " + capSeed.getName());
      }
      // Verify tags
      List<String> tmpTags = tmpSys.getTags();
      Assert.assertNotNull(tmpTags, "Tags value was null");
      Assert.assertEquals(tmpTags.size(), tags2.size(), "Wrong number of tags");
      for (String tagStr : tags2)
      {
        Assert.assertTrue(tmpTags.contains(tagStr));
        System.out.println("Found tag: " + tagStr);
      }
      // Verify notes
      String tmpNotesStr = (String) tmpSys.getNotes();
      JsonObject tmpNotes = ClientTapisGsonUtils.getGson().fromJson(tmpNotesStr, JsonObject.class);
      Assert.assertNotNull(tmpNotes);
      System.out.println("Found notes: " + tmpNotesStr);
      Assert.assertTrue(tmpNotes.has("project"));
      Assert.assertEquals(tmpNotes.get("project").getAsString(), notes2JO.get("project").getAsString());
      Assert.assertTrue(tmpNotes.has("testdata"));
      Assert.assertEquals(tmpNotes.get("testdata").getAsString(), notes2JO.get("testdata").getAsString());
    } catch (Exception e) {
      System.out.println("Caught exception: " + e);
      Assert.fail();
    }
  }

  @Test
  public void testChangeOwner() throws Exception {
    // Create the system
    String[] sys0 = systems.get(16);
    String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, null, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    TSystem tmpSys = getClientUsr(serviceURL, ownerUserJWT).getSystemByName(sys0[1]);
    Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0[1]);
    getClientUsr(serviceURL, ownerUserJWT).changeSystemOwner(sys0[1], newOwnerUser);
    // Now that owner has given away ownership we need to be newOwnerUser or admin to get the system
    tmpSys = Utils.getClientUsr(serviceURL, newOwnerUserJWT).getSystemByName(sys0[1]);
    Assert.assertNotNull(tmpSys, "Unable to get system after change of owner. System: " + sys0[1]);
    System.out.println("Found item: " + sys0[1]);
    Assert.assertEquals(tmpSys.getOwner(), newOwnerUser);
  }

  // Test retrieving a system using only the name. No credentials returned.
  @Test
  public void testGetSystemByNameOnly() throws Exception {
    String[] sys0 = systems.get(13);
    Credential cred0 = SystemsClient.buildCredential(sys0[7], "fakePrivateKey", "fakePublicKey",
            "fakeAccessKey", "fakeAccessSecret", "fakeCert");
    String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    TSystem tmpSys = getClientUsr(serviceURL, ownerUserJWT).getSystemByName(sys0[1]);
    Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0[1]);
    System.out.println("Found item: " + sys0[1]);
//    sys2 = {tenantName, "Csys2", "description 2", sysType, sysOwner, "host2", "effUser2", "fakePassword2",
//            "bucket2", "/root2", "jobLocalWorkDir2", "jobLocalArchDir2", "jobRemoteArchSystem2", "jobRemoteArchDir2"};
    Assert.assertEquals(tmpSys.getName(), sys0[1]);
    Assert.assertEquals(tmpSys.getDescription(), sys0[2]);
    Assert.assertEquals(tmpSys.getSystemType().name(), sys0[3]);
    Assert.assertEquals(tmpSys.getOwner(), sys0[4]);
    Assert.assertEquals(tmpSys.getHost(), sys0[5]);
    Assert.assertEquals(tmpSys.getEffectiveUserId(), sys0[6]);
    Assert.assertEquals(tmpSys.getBucketName(), sys0[8]);
    Assert.assertEquals(tmpSys.getRootDir(), sys0[9]);
    Assert.assertEquals(tmpSys.getJobLocalWorkingDir(), sys0[10]);
    Assert.assertEquals(tmpSys.getJobLocalArchiveDir(), sys0[11]);
    Assert.assertEquals(tmpSys.getJobRemoteArchiveSystem(), sys0[12]);
    Assert.assertEquals(tmpSys.getJobRemoteArchiveDir(), sys0[13]);
    Assert.assertEquals(tmpSys.getDefaultAccessMethod().name(), prot1AccessMethod.name());
    Assert.assertEquals(tmpSys.getPort().intValue(), prot1Port);
    Assert.assertEquals(tmpSys.getUseProxy().booleanValue(), prot1UseProxy);
    Assert.assertEquals(tmpSys.getProxyHost(), prot1ProxyHost);
    Assert.assertEquals(tmpSys.getProxyPort().intValue(), prot1ProxyPort);
    Assert.assertNull(tmpSys.getAccessCredential(), "AccessCredential should be null");
  }

  @Test
  public void testGetSystems() throws Exception {
    // Create 2 systems
    String[] sys0 = systems.get(3);
    Credential cred0 = null;
    String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
    sys0 = systems.get(4);
    respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);

    // Get list of all system names
    List<TSystem> systemsList = getClientUsr(serviceURL, ownerUserJWT).getSystems(null);
    Assert.assertNotNull(systemsList);
    Assert.assertFalse(systemsList.isEmpty());
    var systemNames = new ArrayList<String>();
    for (TSystem system : systemsList) {
      System.out.println("Found item: " + system.getName());
      systemNames.add(system.getName());
    }
    Assert.assertTrue(systemNames.contains(systems.get(3)[1]), "List of systems did not contain system name: " + systems.get(3)[1]);
    Assert.assertTrue(systemNames.contains(systems.get(4)[1]), "List of systems did not contain system name: " + systems.get(4)[1]);
  }

  @Test
  public void testDelete() throws Exception {
    // Create the system
    String[] sys0 = systems.get(6);
    Credential cred0 = null;
    String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
    Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);

    // Delete the system
    getClientUsr(serviceURL, ownerUserJWT).deleteSystemByName(sys0[1]);
    try {
      TSystem tmpSys2 = getClientUsr(serviceURL, ownerUserJWT).getSystemByName(sys0[1]);
      Assert.fail("System not deleted. System name: " + sys0[1]);
    } catch (TapisClientException e) {
      Assert.assertEquals(e.getCode(), 404);
    }
  }

  // Test creating, reading and deleting user permissions for a system
  @Test
  public void testUserPerms() {
    String[] sys0 = systems.get(10);
    Credential cred0 = null;
    // Create a system
    System.out.println("Creating system with name: " + sys0[1]);
    try {
      String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, cred0, prot1TxfrMethodsC);
      System.out.println("Created system: " + respUrl);
      System.out.println("Testing perms for user: " + newPermsUser);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
      // Create user perms for the system
      getClientUsr(serviceURL, ownerUserJWT).grantUserPermissions(sys0[1], newPermsUser, testPerms);
      // Get the system perms for the user and make sure permissions are there
      List<String> userPerms = getClientUsr(serviceURL, ownerUserJWT).getSystemPermissions(sys0[1], newPermsUser);
      Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
      for (String perm : userPerms) {
        System.out.println("After grant found user perm: " + perm);
      }
      Assert.assertEquals(userPerms.size(), testPerms.size(), "Incorrect number of perms returned.");
      for (String perm : testPerms) {
        if (!userPerms.contains(perm)) Assert.fail("User perms should contain permission: " + perm);
      }
      // Remove perms for the user
      getClientUsr(serviceURL, ownerUserJWT).revokeUserPermissions(sys0[1], newPermsUser, testPerms);
      // Get the system perms for the user and make sure permissions are gone.
      userPerms = getClientUsr(serviceURL, ownerUserJWT).getSystemPermissions(sys0[1], newPermsUser);
      Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
      for (String perm : userPerms) {
        System.out.println("After revoke found user perm: " + perm);
      }
      for (String perm : testPerms) {
        if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm);
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  // Test creating, reading and deleting user credentials for a system after system created
  @Test
  public void testUserCredentials()
  {
    // Create a system
    String[] sys0 = systems.get(12);
    System.out.println("Creating system with name: " + sys0[1]);
    try {
      String respUrl = Utils.createSystem(getClientUsr(serviceURL, ownerUserJWT), sys0, prot1Port, prot1AccessMethod, null, prot1TxfrMethodsC);
      System.out.println("Created system: " + respUrl);
      System.out.println("Testing credentials for user: " + newPermsUser);
      Assert.assertFalse(StringUtils.isBlank(respUrl), "Invalid response: " + respUrl);
      ReqCreateCredential reqCred = new ReqCreateCredential();
      reqCred.password(sys0[7]).privateKey("fakePrivateKey").publicKey("fakePublicKey")
           .accessKey("fakeAccessKey").accessSecret("fakeAccessSecret").certificate("fakeCert");
      // Store and retrieve multiple secret types: password, ssh keys, access key and secret
      getClientUsr(serviceURL, ownerUserJWT).updateUserCredential(sys0[1], newPermsUser, reqCred);
      Credential cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.PASSWORD);
      // Verify credentials
      Assert.assertEquals(cred1.getPassword(), reqCred.getPassword());
      cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.PKI_KEYS);
      Assert.assertEquals(cred1.getPublicKey(), reqCred.getPublicKey());
      Assert.assertEquals(cred1.getPrivateKey(), reqCred.getPrivateKey());
      cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.ACCESS_KEY);
      Assert.assertEquals(cred1.getAccessKey(), reqCred.getAccessKey());
      Assert.assertEquals(cred1.getAccessSecret(), reqCred.getAccessSecret());
      // Verify we get credentials for default accessMethod if we do not specify an access method
      cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser);
      Assert.assertEquals(cred1.getPublicKey(), reqCred.getPublicKey());
      Assert.assertEquals(cred1.getPrivateKey(), reqCred.getPrivateKey());

      // Delete credentials and verify they were destroyed
      getClientUsr(serviceURL, ownerUserJWT).deleteUserCredential(sys0[1], newPermsUser);
      try {
        cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.PASSWORD);
      } catch (TapisClientException tce) {
        Assert.assertTrue(tce.getTapisMessage().startsWith("SYSAPI_CRED_NOT_FOUND"), "Wrong exception message: " + tce.getTapisMessage());
        cred1 = null;
      }
      Assert.assertNull(cred1, "Credential not deleted. System name: " + sys0[1] + " User name: " + newPermsUser);

      // Attempt to delete again, should not throw an exception
      getClientUsr(serviceURL, ownerUserJWT).deleteUserCredential(sys0[1], newPermsUser);

      // Set just ACCESS_KEY only and test
      reqCred = new ReqCreateCredential().accessKey("fakeAccessKey2").accessSecret("fakeAccessSecret2");
      getClientUsr(serviceURL, ownerUserJWT).updateUserCredential(sys0[1], newPermsUser, reqCred);
      cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.ACCESS_KEY);
      Assert.assertEquals(cred1.getAccessKey(), reqCred.getAccessKey());
      Assert.assertEquals(cred1.getAccessSecret(), reqCred.getAccessSecret());
      // Attempt to retrieve secret that has not been set
      try {
        cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.PKI_KEYS);
      } catch (TapisClientException tce) {
        Assert.assertTrue(tce.getTapisMessage().startsWith("SYSAPI_CRED_NOT_FOUND"), "Wrong exception message: " + tce.getTapisMessage());
        cred1 = null;
      }
      Assert.assertNull(cred1, "Credential was non-null for missing secret. System name: " + sys0[1] + " User name: " + newPermsUser);
      // Delete credentials and verify they were destroyed
      getClientUsr(serviceURL, ownerUserJWT).deleteUserCredential(sys0[1], newPermsUser);
      try {
        cred1 = getClientFilesSvc().getUserCredential(sys0[1], newPermsUser, AccessMethod.ACCESS_KEY);
      } catch (TapisClientException tce) {
        Assert.assertTrue(tce.getTapisMessage().startsWith("SYSAPI_CRED_NOT_FOUND"), "Wrong exception message: " + tce.getTapisMessage());
        cred1 = null;
      }
      Assert.assertNull(cred1, "Credential not deleted. System name: " + sys0[1] + " User name: " + newPermsUser);
      // Attempt to retrieve secret from non-existent system
      try {
        cred1 = getClientFilesSvc().getUserCredential("AMissingSystemName", newPermsUser, AccessMethod.PKI_KEYS);
      } catch (TapisClientException tce) {
        Assert.assertTrue(tce.getTapisMessage().startsWith("SYSAPI_NOSYSTEM"), "Wrong exception message: " + tce.getTapisMessage());
        cred1 = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @AfterSuite
  public void tearDown() {
// Currently no way to hard delete from client (by design)
//    System.out.println("****** Executing AfterSuite teardown method for class: " + this.getClass().getSimpleName());
//    // TODO: Run SQL to hard delete objects
//    //Remove all objects created by tests, ignore any exceptions
//    for (int i = 0; i < numSystems; i++)
//    {
//      try
//      {
//        getClientUsr(serviceURL, ownerUserJWT).deleteSystemByName(systems.get(i)[1]);
//      } catch (Exception e)
//      {
//      }
//    }
  }

  private static ReqUpdateSystem createPatchSystem(String[] sys)
  {
    ReqUpdateSystem pSys = new ReqUpdateSystem();
    pSys.description(sys[2]);
    pSys.host(sys[5]);
    pSys.enabled(false);
    pSys.effectiveUserId(sys[6]);
    pSys.defaultAccessMethod(ReqUpdateSystem.DefaultAccessMethodEnum.valueOf(prot2AccessMethod.name()));
    pSys.transferMethods(prot2TxfrMethodsU);
    pSys.port(prot2Port).useProxy(prot2UseProxy).proxyHost(prot2ProxyHost).proxyPort(prot2ProxyPort);
    pSys.jobCapabilities(jobCaps2);
    pSys.tags(tags2);
    pSys.notes(notes2JO);
    return pSys;
  }

  private SystemsClient getClientFilesSvc()
  {
    // Create the client each time due to issue with setting different headers needed by svc vs usr client
    SystemsClient clt = new SystemsClient(serviceURL, filesServiceJWT);
    clt.addDefaultHeader("X-Tapis-User", ownerUser1);
    clt.addDefaultHeader("X-Tapis-Tenant", tenantName);
    return clt;
  }
}

