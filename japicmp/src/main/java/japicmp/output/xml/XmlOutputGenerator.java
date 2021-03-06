package japicmp.output.xml;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import japicmp.config.Options;
import japicmp.filter.Filter;
import japicmp.exception.JApiCmpException;
import japicmp.exception.JApiCmpException.Reason;
import japicmp.model.JApiClass;
import japicmp.output.OutputFilter;
import japicmp.output.OutputGenerator;
import japicmp.output.extapi.jpa.JpaAnalyzer;
import japicmp.output.extapi.jpa.model.JpaTable;
import japicmp.output.xml.model.JApiCmpXmlRoot;
import japicmp.util.ListJoiner;
import japicmp.util.Streams;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlOutputGenerator extends OutputGenerator<XmlOutput> {
	private static final String XSD_FILENAME = "japicmp.xsd";
	private static final String XML_SCHEMA = XSD_FILENAME;
	private static final Logger LOGGER = Logger.getLogger(XmlOutputGenerator.class.getName());
	private final boolean createSchemaFile;

	public XmlOutputGenerator(List<JApiClass> jApiClasses, Options options, boolean createSchemaFile) {
		super(options, jApiClasses);
		this.createSchemaFile = createSchemaFile;
	}

	@Override
	public XmlOutput generate() {
		JApiCmpXmlRoot jApiCmpXmlRoot = createRootElement(jApiClasses, options);
		//analyzeJpaAnnotations(jApiCmpXmlRoot, jApiClasses);
		filterClasses(jApiClasses, options);
		return createXmlDocumentAndSchema(options, jApiCmpXmlRoot);
	}

	public static List<File> writeToFiles(Options options, XmlOutput xmlOutput) {
		List<File> filesWritten = new ArrayList<>();
		try {
			if (xmlOutput.getXmlOutputStream().isPresent() && options.getXmlOutputFile().isPresent()) {
				File xmlFile = new File(options.getXmlOutputFile().get());
				try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
					ByteArrayOutputStream outputStream = xmlOutput.getXmlOutputStream().get();
					outputStream.writeTo(fos);
					filesWritten.add(xmlFile);
				} catch (IOException e) {
					throw new JApiCmpException(JApiCmpException.Reason.IoException, "Failed to write XML file '" + xmlFile.getAbsolutePath() + "': " + e.getMessage(), e);
				}
			}
			if (xmlOutput.getHtmlOutputStream().isPresent() && options.getHtmlOutputFile().isPresent()) {
				File htmlFile = new File(options.getHtmlOutputFile().get());
				try (FileOutputStream fos = new FileOutputStream(htmlFile)) {
					ByteArrayOutputStream outputStream = xmlOutput.getHtmlOutputStream().get();
					outputStream.writeTo(fos);
					filesWritten.add(htmlFile);
				} catch (IOException e) {
					throw new JApiCmpException(JApiCmpException.Reason.IoException, "Failed to write HTML file '" + htmlFile.getAbsolutePath() + "': " + e.getMessage(), e);
				}
			}
		} finally {
			try {
				xmlOutput.close();
			} catch (Exception ignored) {}
		}
		return filesWritten;
	}

	private void analyzeJpaAnnotations(JApiCmpXmlRoot jApiCmpXmlRoot, List<JApiClass> jApiClasses) {
		JpaAnalyzer jpaAnalyzer = new JpaAnalyzer();
		List<JpaTable> jpaEntities = jpaAnalyzer.analyze(jApiClasses);
		//jApiCmpXmlRoot.setJpaTables(jpaEntities);
	}

	private XmlOutput createXmlDocumentAndSchema(Options options, JApiCmpXmlRoot jApiCmpXmlRoot) {
		XmlOutput xmlOutput = new XmlOutput();
		xmlOutput.setJApiCmpXmlRoot(jApiCmpXmlRoot);
		ByteArrayOutputStream xmlBaos = null;
		InputStream styleSheetAsInputStream = null;
		InputStream xsltAsInputStream = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(JApiCmpXmlRoot.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, XML_SCHEMA);
			xmlBaos = new ByteArrayOutputStream();
			marshaller.marshal(jApiCmpXmlRoot, xmlBaos);
			if (options.getXmlOutputFile().isPresent()) {
				xmlOutput.setXmlOutputStream(Optional.of(xmlBaos));
				if (this.createSchemaFile) {
					final File xmlFile = new File(options.getXmlOutputFile().get());
					SchemaOutputResolver outputResolver = new SchemaOutputResolver() {
						@Override
						public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
							File schemaFile = xmlFile.getParentFile();
							if (schemaFile == null) {
								LOGGER.warning(String.format("File '%s' has no parent file. Using instead: '%s'.", xmlFile.getAbsolutePath(), XSD_FILENAME));
								schemaFile = new File(XSD_FILENAME);
							} else {
								schemaFile = new File(schemaFile + File.separator + XSD_FILENAME);
							}
							StreamResult result = new StreamResult(schemaFile);
							result.setSystemId(schemaFile.getAbsolutePath());
							return result;
						}
					};
					jaxbContext.generateSchema(outputResolver);
				}
			}
			if (options.getHtmlOutputFile().isPresent()) {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				xsltAsInputStream = XmlOutputGenerator.class.getResourceAsStream("/html.xslt");
				if (xsltAsInputStream == null) {
					throw new JApiCmpException(Reason.XsltError, "Failed to load XSLT.");
				}
				if (options.getHtmlStylesheet().isPresent()) {
					styleSheetAsInputStream = new FileInputStream(options.getHtmlStylesheet().get());
				} else {
					styleSheetAsInputStream = XmlOutputGenerator.class.getResourceAsStream("/style.css");
					if (styleSheetAsInputStream == null) {
						throw new JApiCmpException(Reason.XsltError, "Failed to load stylesheet.");
					}
				}
				String xsltAsString = integrateStylesheetIntoXslt(xsltAsInputStream, styleSheetAsInputStream);
				Transformer transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(xsltAsString)));
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlBaos.toByteArray());
				ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();
				transformer.transform(new StreamSource(byteArrayInputStream), new StreamResult(htmlOutputStream));
				xmlOutput.setHtmlOutputStream(Optional.of(htmlOutputStream));
			}
		} catch (JAXBException e) {
			throw new JApiCmpException(Reason.JaxbException, String.format("Marshalling of XML document failed: %s", e.getMessage()), e);
		} catch (IOException e) {
			throw new JApiCmpException(Reason.IoException, String.format("Marshalling of XML document failed: %s", e.getMessage()), e);
		} catch (TransformerConfigurationException e) {
			throw new JApiCmpException(Reason.XsltError, String.format("Configuration of XSLT transformer failed: %s", e.getMessage()), e);
		} catch (TransformerException e) {
			throw new JApiCmpException(Reason.XsltError, String.format("XSLT transformation failed: %s", e.getMessage()), e);
		} finally {
			try {
				if (styleSheetAsInputStream != null) {
					styleSheetAsInputStream.close();
				}
				if (xsltAsInputStream != null) {
					xsltAsInputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
		return xmlOutput;
	}

	private String integrateStylesheetIntoXslt(InputStream xsltAsInputStream, InputStream styleSheetAsInputStream) {
		String xsltAsString = Streams.asString(xsltAsInputStream);
		String styleSheetAsString = Streams.asString(styleSheetAsInputStream);
		xsltAsString = xsltAsString.replace("<style type=\"text/css\"></style>", "<style type=\"text/css\">\n" + styleSheetAsString + "\n</style>");
		if (System.getProperty("japicmp.dump.xslt") != null) {
			try {
				Files.write(Paths.get(System.getProperty("japicmp.dump.xslt")), Arrays.asList(xsltAsString), Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Could not dump XSLT file: " + e.getMessage(), e);
			}
		}
		return xsltAsString;
	}

	private void filterClasses(List<JApiClass> jApiClasses, Options options) {
		OutputFilter outputFilter = new OutputFilter(options);
		outputFilter.filter(jApiClasses);
	}

	private JApiCmpXmlRoot createRootElement(List<JApiClass> jApiClasses, Options options) {
		ListJoiner<File> joiner = new ListJoiner<File>().on(";").sort(new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		}).toStringBuilder(new ListJoiner.ListJoinerToString<File>() {
			@Override
			public String toString(File file) {
				return file.getAbsolutePath();
			}
		});
		JApiCmpXmlRoot jApiCmpXmlRoot = new JApiCmpXmlRoot();
		jApiCmpXmlRoot.setOldJar(joiner.join(options.getOldArchives()));
		jApiCmpXmlRoot.setNewJar(joiner.join(options.getNewArchives()));
		jApiCmpXmlRoot.setClasses(jApiClasses);
		jApiCmpXmlRoot.setAccessModifier(options.getAccessModifier().name());
		jApiCmpXmlRoot.setOnlyModifications(options.isOutputOnlyModifications());
		jApiCmpXmlRoot.setOnlyBinaryIncompatibleModifications(options.isOutputOnlyBinaryIncompatibleModifications());
		jApiCmpXmlRoot.setPackagesInclude(filtersAsString(options.getIncludes(), true));
		jApiCmpXmlRoot.setPackagesExclude(filtersAsString(options.getExcludes(), false));
		jApiCmpXmlRoot.setIgnoreMissingClasses(options.isIgnoreMissingClasses());
		return jApiCmpXmlRoot;
	}

	private String filtersAsString(List<Filter> filters, boolean include) {
		String join;
		if (filters.size() == 0) {
			if (include) {
				join = "all";
			} else {
				join = "n.a.";
			}
		} else {
			join = Joiner.on(";").skipNulls().join(filters);
		}
		return join;
	}
}
