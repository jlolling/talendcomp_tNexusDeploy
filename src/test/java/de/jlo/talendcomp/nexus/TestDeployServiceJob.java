package de.jlo.talendcomp.nexus;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestDeployServiceJob {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSetJobZipFile() {
		DeployServiceJobNexus2 d = new DeployServiceJobNexus2();
		String zipFilePath = "/path/to/my_artifact-1.23.jar";
		d.setJobFile(zipFilePath);
		String expectedArtifactid = "my_artifact";
		String expectedVersion = "1.23.0";
		String actualArtifactId = d.getArtifactId();
		String actualVersion = d.getVersion();
		Assert.assertEquals("ArtifactId does not match", expectedArtifactid, actualArtifactId);
		Assert.assertEquals("Version does not match", expectedVersion, actualVersion);
	}
	
	@Test
	public void testDeloyServicejobNexus3() throws Exception {
		ServiceDeployer d = new DeployServiceJobNexus3();
		d.setNexusUrl("http://localhost:8081");
		d.setNexusUser("admin");
		d.setNexusPasswd("Talend123");
		String zipFilePath = "/Data/exported_jobs/service_ping-0.4.jar";
		d.setJobFile(zipFilePath);
		String expectedArtifactid = "service_ping";
		String expectedVersion = "0.4.0";
		String actualArtifactId = d.getArtifactId();
		String actualVersion = d.getVersion();
		Assert.assertEquals("ArtifactId does not match", expectedArtifactid, actualArtifactId);
		Assert.assertEquals("Version does not match", expectedVersion, actualVersion);
		d.connect();
		d.deployBundleToNexus();
		d.deployFeatureToNexus();
		Assert.assertTrue(true);
	}

}
