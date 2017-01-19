/*
 * Copyright (c) 2010 - 2015 Norwegian Agency for Pupblic Government and eGovernment (Difi)
 *
 * This file is part of Oxalis.
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission
 * - subsequent versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence
 *  is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package eu.peppol.inbound.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.peppol.util.OxalisVersion;
import eu.peppol.util.PropertyDef;
import no.difi.vefa.peppol.mode.Mode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Servlet returning diagnostic information to ease operation, support and debugging.
 * Since this servlet is public accessible, it should NOT contain any sensitive
 * information about it's runtime environment.
 * @author erlend
 * @author thore
 */
@Singleton
public class StatusServlet extends HttpServlet {

    private final X509Certificate certificate;

    private final Mode mode;

    @Inject
    public StatusServlet(X509Certificate certificate, Mode mode) {
        this.certificate = certificate;
        this.mode = mode;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/plain");

        PrintWriter writer = resp.getWriter();
        writer.println("version.oxalis: " + OxalisVersion.getVersion());
        writer.println("version.java: " + System.getProperty("java.version"));
        writer.println(PropertyDef.OPERATION_MODE.getPropertyName() + ": " + mode.getIdentifier());

        if (mode.getConfig().hasPath("lookup.locator.hostname"))
            writer.println(PropertyDef.SML_HOSTNAME.getPropertyName() + ": " + mode.getString("lookup.locator.hostname"));

        writer.println("certificate.subject: " + certificate.getSubjectX500Principal().getName());
        writer.println("certificate.issuer: " + certificate.getIssuerX500Principal().getName());
        writer.println("certificate.expired: " + certificate.getNotAfter().before(new Date()));
        writer.println("build.id: " + OxalisVersion.getBuildId());
        writer.println("build.tstamp: " + OxalisVersion.getBuildTimeStamp());

        // TODO add flag to indicate if OXALIS_HOME is specified or if default is used

    }
}
