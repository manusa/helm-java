package com.marcnuri.helm.jni;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class RemoteJarLoader {

  private static final String TEMP_DIR = "java.io.tmpdir";
  private static final String VERSION = "com.marcnuri.jkube-helm.version";
  private static final String GROUP_ID = "com.marcnuri.jkube-helm.groupId";
  private static final String SNAPSHOTS = "com.marcnuri.jkube-helm.repository.snapshots";
  private static final String RELEASES = "com.marcnuri.jkube-helm.repository.releases";
  // For testing
  private static final String FORCE_UPDATE = "com.marcnuri.jkube-helm.forceUpdate";
  private static final String SKIP = "com.marcnuri.jkube-helm.skipRemoteJar";

  private RemoteJarLoader() {
  }

  static ClassLoader remoteJar() {
    // For testing
    if (System.getProperty(SKIP) != null) {
      return null;
    }
    final Properties nativeLibraries = new Properties();
    try (final InputStream stream = NativeLibrary.class.getResourceAsStream("/META-INF/native-libraries.properties")) {
      nativeLibraries.load(stream);
      final String version;
      // Version can be overridden (for testing) using a system property
      if (System.getProperty(VERSION) != null) {
        version = System.getProperty(VERSION);
      } else {
        version = nativeLibraries.getProperty(VERSION);
      }
      final String groupId = nativeLibraries.getProperty(GROUP_ID);
      final boolean isSnapshot = version.endsWith("-SNAPSHOT");
      final String repository = isSnapshot ?
        nativeLibraries.getProperty(SNAPSHOTS) :
        nativeLibraries.getProperty(RELEASES);
      final String osName = osName();
      final String archName = archName();
      if (osName != null && archName != null) {
        final String groupUrl = repository + "/" + groupId.replace('.', '/') + "/" + osName + "-" + archName + "/" + version;
        final String jarName;
        if (isSnapshot) {
          jarName = latestSnapshot(version, osName, archName, groupUrl);
        } else {
          jarName = osName + "-" + archName + "-" + version + ".jar";
        }
        final URL jarUrl = new URL(groupUrl + "/" + jarName);
        final Path jarFile = Paths.get(System.getProperty(TEMP_DIR), jarName);
        return new URLClassLoader(new URL[]{cache(jarUrl, jarFile)}, NativeLibrary.class.getClassLoader());
      }
    } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException exception) {
      // NO OP
    }
    return null;
  }

  private static String latestSnapshot(String version, String osName, String archName, String groupUrl)
    throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
    final String metadataXml = groupUrl + "/maven-metadata.xml";
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    final Document xml = factory.newDocumentBuilder().parse(metadataXml);
    final XPath xPath = XPathFactory.newInstance().newXPath();
    final String versionFragment = version.replace("-SNAPSHOT", "");
    final String timestamp = xPath.evaluate("//metadata/versioning/snapshot/timestamp", xml);
    final String buildNumber = xPath.evaluate("//metadata/versioning/snapshot/buildNumber", xml);
    return osName + "-" + archName + "-" + versionFragment + "-" + timestamp + "-" + buildNumber + ".jar";
  }

  private static URL cache(URL jarUrl, Path jarFile) throws IOException {
    if (System.getProperty(FORCE_UPDATE) != null || !jarFile.toFile().exists() || !jarFile.toFile().isFile()) {
      Files.deleteIfExists(jarFile);
      try (
        ReadableByteChannel inChannel = Channels.newChannel(jarUrl.openStream());
        FileOutputStream fos = new FileOutputStream(jarFile.toFile());
        FileChannel outChannel = fos.getChannel()
      ) {
        outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
      }
    }
    return jarFile.toUri().toURL();
  }

  private static String osName() {
    final String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return "windows";
    } else if (osName.contains("mac")) {
      return "darwin";
    } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
      return "linux";
    }
    return null;
  }

  private static String archName() {
    final String arch = System.getProperty("os.arch");
    if (arch.equals("amd64") || arch.equals("x86_64")) {
      return "amd64";
    } else if (arch.equals("arch64")) {
      return "arm64";
    }
    return null;
  }
}
