package de.jlo.talendcomp.nexus;

import org.junit.Assert;
import org.junit.Test;

public class TestBatchJobDeployer {

	@Test
	public void testSetJobZipFile() {
		BatchjobDeployer d = new DeployDIJobNexus2();
		String zipFilePath = "/path/to/my_artifact-1.23.zip";
		d.setJobFile(zipFilePath);
		String expectedArtifactid = "my_artifact";
		String expectedVersion = "1.23.0";
		String actualArtifactId = d.getArtifactId();
		String actualVersion = d.getVersion();
		Assert.assertEquals("ArtifactId does not match", expectedArtifactid, actualArtifactId);
		Assert.assertEquals("Version does not match", expectedVersion, actualVersion);
	}
	
	@Test
	public void testDeloyBatchjobNexus3() throws Exception {
		BatchjobDeployer d = new DeployDIJobNexus3();
		d.setNexusUrl("http://localhost:8081");
		d.setNexusUser("admin");
		d.setNexusPasswd("Talend123");
		String zipFilePath = "/Data/exported_jobs/test_calendar_0.1.zip";
		d.setJobFile(zipFilePath);
		String expectedArtifactid = "test_calendar";
		String expectedVersion = "0.1.0";
		String actualArtifactId = d.getArtifactId();
		String actualVersion = d.getVersion();
		Assert.assertEquals("ArtifactId does not match", expectedArtifactid, actualArtifactId);
		Assert.assertEquals("Version does not match", expectedVersion, actualVersion);
		d.connect();
		d.deployJobToNexus();
		Assert.assertTrue(true);
	}
	
}
