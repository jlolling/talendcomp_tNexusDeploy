package de.jlo.talendcomp.nexus;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

public class DeployServiceJobNexus2 extends ServiceDeployer {
	
	protected String nexusRepository = "releases";
	protected String restPath = "/service/local/artifact/maven/content";

	@Override
	public String getNexusVersion() {
		return BatchjobDeployer.NEXUS_2;
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

	private String buildBundlePom() {
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
		pom.append("<packaging>bundle</packaging>\n");
		pom.append("</project>");
		return pom.toString();
	}
	
	private String buildFeaturePom() {
		StringBuilder pom = new StringBuilder();
		pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
		pom.append("<modelVersion>4.0.0</modelVersion>\n");
		pom.append("<groupId>"); 
		pom.append(groupId);
		pom.append("</groupId>\n");
		pom.append("<artifactId>");
		pom.append(artifactId);
		pom.append("-feature</artifactId>\n");
		pom.append("<version>");
		pom.append(version);
		pom.append("</version>\n");
		pom.append("<packaging>pom</packaging>\n");
		pom.append("<type>xml</type>\n");
		pom.append("<classifier>features</classifier>\n");
		pom.append("<dependencies>\n");
		pom.append("<dependency>\n");
		pom.append("<groupId>"); 
		pom.append(groupId);
		pom.append("</groupId>\n");
		pom.append("<artifactId>");
		pom.append(artifactId);
		pom.append("</artifactId>\n");
		pom.append("<version>");
		pom.append(version);
		pom.append("</version>\n");
		pom.append("</dependency>\n");
		pom.append("</dependencies>\n");
		pom.append("</project>");
		return pom.toString();
	}

	private String buildFeatureXML() {
		StringBuilder xml = new StringBuilder();
		xml.append("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\" name=\"");
		xml.append(artifactId);
		xml.append("-feature");
		xml.append("\">\n");
		xml.append("<feature name=\"");
		xml.append(artifactId);
		xml.append("-feature\"");
		xml.append(" version=\"");
		xml.append(version);
		xml.append("\">\n"); 
		xml.append("<bundle>");
		xml.append("mvn:");
		xml.append(groupId);
		xml.append("/");
		xml.append(artifactId);
		xml.append("/");
		xml.append(version);
		xml.append("</bundle>\n");
		xml.append("<config name=\"");
		xml.append(artifactId);
		xml.append(".talendcontext.Default\">\n");
		xml.append("</config>\n");
		xml.append("</feature>\n");
		xml.append("</features>");
		return xml.toString();
	}

	@Override
	public void deployBundleToNexus() throws Exception {
		checkJobFile();
		String pom = buildBundlePom();
		HttpPost post = new HttpPost(nexusUrl + restPath);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addTextBody("r", nexusRepository, ContentType.DEFAULT_TEXT);
		builder.addTextBody("hasPom", "true", ContentType.DEFAULT_TEXT);
		builder.addTextBody("e", "jar", ContentType.DEFAULT_TEXT);
		builder.addBinaryBody("file", pom.getBytes("UTF-8"), ContentType.DEFAULT_BINARY, "pom.xml");
		InputStream inputStream = new FileInputStream(jobFile);
		builder.addBinaryBody("file", inputStream, ContentType.DEFAULT_BINARY, jobFile.getName());
		HttpEntity entity = builder.build();
		post.setEntity(entity);
		String response = null;
		try {
			response = httpClient.execute(post, false);			
		} catch (Exception e) {
			throw new Exception("Deploy bundle: \n" + pom + "\nfailed. Response: " + response, e);
		}
		if (deleteLocalArtifactFile && (httpClient.getStatusCode() >= 200 || httpClient.getStatusCode() <= 204)) {
			deleteLocalFile();
		}
	}

	@Override
	public void deployFeatureToNexus() throws Exception {
		String pom = buildFeaturePom();
		String feature = buildFeatureXML();
		HttpPost post = new HttpPost(nexusUrl + restPath);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addTextBody("r", nexusRepository, ContentType.DEFAULT_TEXT);
		builder.addTextBody("hasPom", "true", ContentType.DEFAULT_TEXT);
		builder.addTextBody("e", "xml", ContentType.DEFAULT_TEXT);
		builder.addBinaryBody("file", pom.getBytes("UTF-8"), ContentType.DEFAULT_BINARY, "pom.xml");
		builder.addBinaryBody("file", feature.getBytes("UTF-8"), ContentType.DEFAULT_BINARY, "feature.xml");
		HttpEntity entity = builder.build();
		post.setEntity(entity);
		String response = null;
		try {
			response = httpClient.execute(post, false);			
		} catch (Exception e) {
			throw new Exception("Deploy feature: \n" + feature + "\nfailed. Response: " + response, e);
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
