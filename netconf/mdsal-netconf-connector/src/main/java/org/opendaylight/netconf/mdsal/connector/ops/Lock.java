/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.mdsal.connector.ops;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/* FIXME Duplicated code
   netconf/netconf/config-netconf-connector/src/main/java/org/opendaylight/netconf/
   confignetconfconnector/operations/Lock.java
 */
public class Lock extends AbstractSingletonNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(Lock.class);

    private static final String OPERATION_NAME = "lock";
    private static final String TARGET_KEY = "target";

    public Lock(final String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement)
            throws DocumentedException {
        final Datastore targetDatastore = extractTargetParameter(operationElement);
        if (targetDatastore == Datastore.candidate) {
            LOG.debug("Locking candidate datastore on session: {}", getNetconfSessionIdForReporting());
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        }

        throw new DocumentedException("Unable to lock " + targetDatastore + " datastore",
                DocumentedException.ErrorType.APPLICATION, DocumentedException.ErrorTag.OPERATION_NOT_SUPPORTED,
                DocumentedException.ErrorSeverity.ERROR);
    }

    static Datastore extractTargetParameter(final XmlElement operationElement) throws DocumentedException {
        final XmlElement targetElement = operationElement.getOnlyChildElementWithSameNamespace(TARGET_KEY);
        final XmlElement targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();
        return Datastore.valueOf(targetChildNode.getName());
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }

}
