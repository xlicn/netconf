/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040;

import static org.junit.Assert.assertNotNull;

import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class TestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    public static SchemaContext loadSchemaContext(final String... yangPath)
            throws FileNotFoundException, ReactorException {
        final List<File> files = new ArrayList<>();
        for (final String path : yangPath) {
            final String pathToFile = TestUtils.class.getResource(path).getPath();
            final File testDir = new File(pathToFile);
            final String[] fileList = testDir.list();
            if (fileList == null) {
                throw new FileNotFoundException(pathToFile);
            }

            for (final String fileName : fileList) {
                final File file = new File(testDir, fileName);
                if (file.isDirectory() == false) {
                    files.add(file);
                }
            }
        }

        return YangParserTestUtils.parseYangSources(files);
    }

    public static Module findModule(final Set<Module> modules, final String moduleName) {
        for (final Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public static Document loadDocumentFrom(final InputStream inputStream) {
        try {
            return UntrustedXML.newDocumentBuilder().parse(inputStream);
        } catch (SAXException | IOException e) {
            LOG.error("Error during loading Document from XML", e);
            return null;
        }
    }

    public static String getDocumentInPrintableForm(final Document doc) {
        Preconditions.checkNotNull(doc);
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out,
                StandardCharsets.UTF_8)));
            final byte[] charData = out.toByteArray();
            return new String(charData, StandardCharsets.UTF_8);
        } catch (final TransformerException e) {
            final String msg = "Error during transformation of Document into String";
            LOG.error(msg, e);
            return msg;
        }

    }

    /**
     * Searches module with name {@code searchedModuleName} in {@code modules}. If module name isn't specified and
     * module set has only one element then this element is returned.
     *
     */
    public static Module resolveModule(final String searchedModuleName, final Set<Module> modules) {
        assertNotNull("Modules can't be null.", modules);
        if (searchedModuleName != null) {
            for (final Module m : modules) {
                if (m.getName().equals(searchedModuleName)) {
                    return m;
                }
            }
        } else if (modules.size() == 1) {
            return modules.iterator().next();
        }
        return null;
    }

    public static DataSchemaNode resolveDataSchemaNode(final String searchedDataSchemaName, final Module module) {
        assertNotNull("Module can't be null", module);

        if (searchedDataSchemaName != null) {
            for (final DataSchemaNode dsn : module.getChildNodes()) {
                if (dsn.getQName().getLocalName().equals(searchedDataSchemaName)) {
                    return dsn;
                }
            }
        } else if (module.getChildNodes().size() == 1) {
            return module.getChildNodes().iterator().next();
        }
        return null;
    }

    public static QName buildQName(final String name, final String uri, final String date, final String prefix) {
        try {
            final URI u = new URI(uri);
            Date dt = null;
            if (date != null) {
                dt = Date.valueOf(date);
            }
            return QName.create(u, dt, name);
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public static QName buildQName(final String name, final String uri, final String date) {
        return buildQName(name, uri, date, null);
    }

    public static QName buildQName(final String name) {
        return buildQName(name, "", null);
    }

    public static String loadTextFile(final String filePath) throws IOException {
        final FileReader fileReader = new FileReader(filePath);
        final BufferedReader bufReader = new BufferedReader(fileReader);

        String line = null;
        final StringBuilder result = new StringBuilder();
        while ((line = bufReader.readLine()) != null) {
            result.append(line);
        }
        bufReader.close();
        return result.toString();
    }

    private static Pattern patternForStringsSeparatedByWhiteChars(final String... substrings) {
        final StringBuilder pattern = new StringBuilder();
        pattern.append(".*");
        for (final String substring : substrings) {
            pattern.append(substring);
            pattern.append("\\s*");
        }
        pattern.append(".*");
        return Pattern.compile(pattern.toString(), Pattern.DOTALL);
    }

    public static boolean containsStringData(final String jsonOutput, final String... substrings) {
        final Pattern pattern = patternForStringsSeparatedByWhiteChars(substrings);
        final Matcher matcher = pattern.matcher(jsonOutput);
        return matcher.matches();
    }

    public static NodeIdentifier getNodeIdentifier(final String localName, final String namespace,
            final String revision) throws ParseException {
        return new NodeIdentifier(QName.create(namespace, revision, localName));
    }

    public static NodeIdentifierWithPredicates getNodeIdentifierPredicate(final String localName,
            final String namespace, final String revision, final Map<String, Object> keys) throws ParseException {
        final Map<QName, Object> predicate = new HashMap<>();
        for (final String key : keys.keySet()) {
            predicate.put(QName.create(namespace, revision, key), keys.get(key));
        }

        return new NodeIdentifierWithPredicates(QName.create(namespace, revision, localName), predicate);
    }

    public static NodeIdentifierWithPredicates getNodeIdentifierPredicate(final String localName,
            final String namespace, final String revision, final String... keysAndValues) throws ParseException {
        Preconditions.checkArgument(keysAndValues.length % 2 == 0,
                "number of keys argument have to be divisible by 2 (map)");
        final Map<QName, Object> predicate = new HashMap<>();

        int index = 0;
        while (index < keysAndValues.length) {
            predicate.put(QName.create(namespace, revision, keysAndValues[index++]), keysAndValues[index++]);
        }

        return new NodeIdentifierWithPredicates(QName.create(namespace, revision, localName), predicate);
    }

    public static NormalizedNode<?, ?> prepareNormalizedNodeWithIetfInterfacesInterfacesData() throws ParseException {
        final String ietfInterfacesDate = "2013-07-04";
        final String namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces";
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntryNode =
                ImmutableMapEntryNodeBuilder.create();

        final Map<String, Object> predicates = new HashMap<>();
        predicates.put("name", "eth0");

        mapEntryNode.withNodeIdentifier(getNodeIdentifierPredicate("interface", namespace, ietfInterfacesDate,
                predicates));
        mapEntryNode
                .withChild(new ImmutableLeafNodeBuilder<String>()
                        .withNodeIdentifier(getNodeIdentifier("name", namespace, ietfInterfacesDate)).withValue("eth0")
                        .build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(getNodeIdentifier("type", namespace, ietfInterfacesDate))
                .withValue("ethernetCsmacd").build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<Boolean>()
                .withNodeIdentifier(getNodeIdentifier("enabled", namespace, ietfInterfacesDate))
                .withValue(Boolean.FALSE).build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(getNodeIdentifier("description", namespace, ietfInterfacesDate))
                .withValue("some interface").build());

        return mapEntryNode.build();
    }
}
