package de.jlo.talendcomp.nexus;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

public class DeployDIJobNexus2 extends BatchjobDeployer {

	protected String nexusRepository = "job-releases";
	protected String restPath = "/service/local/artifact/maven/content";

	@Override
	public String getNexusVersion() {
		return NEXUS_2;
	}
	
	@Override
	public String getNexusRepository() {
		return nexusRepository;
	}

	@Override
	public void setNexusRepository(String repo) {
		if (repo == null || repo.trim().isEmpty()) {
			throw new IllegalArgumentException("Repo cannot be null or empty");
		}
		this.nexusRepository = repo;
	}

	private String buildDIJobPom() {
		StringBuilder pom = new StringBuilder();
		pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
		pom.append("<modelVersion>4.0.0</modelVersion>\n");
		pom.append("<groupId>");
		pom.append(groupId);
		pom.append("</groupId>\n");
		pom.append("<artifactId>");
		pom.append(artifactId);
		pom.append("</artifactId>\n");
		pom.append("<version>");
		pom.append(version);
		pom.append("</version>\n");
		pom.append("<type>zip</type>\n");
		pom.append("</project>");
		return pom.toString();
	}
	
	@Override
	public void deployJobToNexus() throws Exception {
		checkJobFile();
		if (httpClient == null) {
			throw new IllegalStateException("Http client not connected");
		}
		HttpPost post = new HttpPost(nexusUrl + restPath);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addTextBody("r", nexusRepository, ContentType.DEFAULT_TEXT);
		builder.addTextBody("hasPom", "true", ContentType.DEFAULT_TEXT);
		builder.addTextBody("e", "zip", ContentType.DEFAULT_TEXT);
		builder.addBinaryBody("file", buildDIJobPom().getBytes("UTF-8"), ContentType.DEFAULT_BINARY, "pom.xml");
		InputStream inputStream = new FileInputStream(getJobFile());
		builder.addBinaryBody("file", inputStream, ContentType.DEFAULT_BINARY, getJobFile().getName());
		HttpEntity entity = builder.build();
		post.setEntity(entity);
		String response = httpClient.execute(post, false);
		if (httpClient.getStatusCode() >= 200 || httpClient.getStatusCode() <= 204) {
			if (deleteLocalArtifactFile) {
				deleteLocalFile();
			}
		} else if (httpClient.getStatusCode() > 204) {
			System.err.println(response);
		}
	}

	@Override
	public boolean checkIfArtifactAlreadyExists() throws Exception {
		if (artifactId == null) {
			throw new IllegalStateException("artifactId is null. You have to call setJobFile before");
		}
		if (version == null) {
			throw new IllegalStateException("version is null. You have to call setJobFile before");
		}
		if (groupId == null) {
			throw new IllegalStateException("groupId is null but it is mandatory.");
		}
		String checkPomUrl = nexusUrl + "/service/local/repositories/" + nexusRepository + "/content/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
		HttpGet get = new HttpGet(checkPomUrl);
		try {
			httpClient.execute(get, false);
			if (httpClient.getStatusCode() == 200) {
				return true;
			}
		} catch (Exception e) {
			if (httpClient.getStatusCode() == 404) {
				return false;
			} else {
				throw new Exception("Check exist for groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + " failed. uri: " + get.getURI(), e);
			}
		}
		return false;
	}

}