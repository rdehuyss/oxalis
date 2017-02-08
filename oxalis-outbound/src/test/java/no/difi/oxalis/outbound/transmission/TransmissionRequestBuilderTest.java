/*
 * Copyright 2010-2017 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.oxalis.outbound.transmission;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import eu.peppol.identifier.*;
import no.difi.oxalis.api.lang.OxalisException;
import no.difi.oxalis.api.outbound.TransmissionRequest;
import no.difi.oxalis.commons.guice.GuiceModuleLoader;
import no.difi.oxalis.sniffer.PeppolStandardBusinessHeader;
import no.difi.oxalis.test.lookup.MockLookupModule;
import no.difi.vefa.peppol.common.model.Header;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.TransportProfile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * These tests needs TransmissionRequestBuilder to run in TEST-mode to be able to override values
 *
 * @author steinar
 * @author thore
 */
@Guice(modules = GuiceModuleLoader.class)
public class TransmissionRequestBuilderTest {

    @Inject
    @Named("test-files-with-identification")
    public Map<String, PeppolStandardBusinessHeader> testFilesForIdentification;

    @Inject
    @Named("test-non-ubl-documents")
    public Map<String, PeppolStandardBusinessHeader> testNonUBLFiles;

    @Inject
    @Named("sample-xml-with-sbdh")
    InputStream inputStreamWithSBDH;

    @Inject
    @Named("sample-xml-no-sbdh")
    InputStream noSbdhInputStream;

    @Inject
    @Named("sample-xml-missing-metadata")
    InputStream missingMetadataInputStream;

    @Inject
    TransmissionRequestBuilder transmissionRequestBuilder;

    @BeforeMethod
    public void setUp() {
        MockLookupModule.resetService();

        // The GlobalConfiguration object referenced by TransmissionRequestBuilder is a singleton
        // hence we must ensure it has the value expected by us.
        transmissionRequestBuilder.setTransmissionBuilderOverride(true);

        inputStreamWithSBDH.mark(Integer.MAX_VALUE);
        noSbdhInputStream.mark(Integer.MAX_VALUE);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        inputStreamWithSBDH.reset();
        noSbdhInputStream.reset();
    }

    @Test
    public void makeSureWeAllowOverrides() {
        assertNotNull(transmissionRequestBuilder);
        assertTrue(transmissionRequestBuilder.isOverrideAllowed(), "Overriding transmission request parameters is not permitted!");
    }

    @Test
    public void createTransmissionRequestBuilderWithOnlyTheMessageDocument() throws Exception {

        assertNotNull(transmissionRequestBuilder);
        assertNotNull(inputStreamWithSBDH);

        transmissionRequestBuilder.payLoad(inputStreamWithSBDH);

        // Builds the actual transmission request
        TransmissionRequest transmissionRequest = transmissionRequestBuilder.build();

        PeppolStandardBusinessHeader sbdh = transmissionRequestBuilder.getEffectiveStandardBusinessHeader();
        assertNotNull(sbdh);
        assertEquals(sbdh.getRecipientId(), WellKnownParticipant.DIFI_TEST);

        assertNotNull(transmissionRequest.getEndpoint());

        assertNotNull(transmissionRequest.getHeader());

        assertEquals(transmissionRequest.getHeader().getReceiver(), WellKnownParticipant.DIFI_TEST.toVefa());

        assertEquals(transmissionRequest.getEndpoint().getTransportProfile(),
                TransportProfile.of("busdox-transport-as2-ver1p0"));

        assertNotNull(transmissionRequest.getHeader().getIdentifier());
    }

    @Test
    public void xmlWithNoSBDH() throws Exception {

        TransmissionRequestBuilder builder = transmissionRequestBuilder.payLoad(noSbdhInputStream).receiver(WellKnownParticipant.DIFI_TEST);
        TransmissionRequest request = builder.build();

        assertNotNull(builder);
        assertNotNull(builder.getEffectiveStandardBusinessHeader(), "Effective SBDH is null");

        assertEquals(builder.getEffectiveStandardBusinessHeader().getRecipientId(), WellKnownParticipant.DIFI_TEST, "Receiver has not been overridden");
        assertEquals(request.getHeader().getReceiver(), WellKnownParticipant.DIFI_TEST.toVefa());

    }


    @Test
    public void overrideFields() throws Exception {

        TransmissionRequestBuilder builder = transmissionRequestBuilder.payLoad(noSbdhInputStream)
                .sender(WellKnownParticipant.DIFI_TEST)
                .receiver(WellKnownParticipant.U4_TEST)
                .documentType(PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier());

        TransmissionRequest request = builder.build();

        assertEquals(request.getEndpoint().getTransportProfile(), TransportProfile.of("busdox-transport-as2-ver1p0"));
        assertEquals(request.getHeader().getReceiver(), WellKnownParticipant.U4_TEST.toVefa());
        assertEquals(request.getHeader().getSender(), WellKnownParticipant.DIFI_TEST.toVefa());
        assertEquals(request.getHeader().getDocumentType(),
                PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier().toVefa());
    }

    @Test
    public void testOverrideEndPoint() throws Exception {
        assertNotNull(inputStreamWithSBDH);
        URI url = URI.create("http://localhost:8080/oxalis/as2");
        TransmissionRequest request = transmissionRequestBuilder
                .payLoad(inputStreamWithSBDH)
                .overrideAs2Endpoint(url, "APP_1000000006").build();
        assertEquals(request.getEndpoint().getTransportProfile(), TransportProfile.AS2_1_0);
        assertEquals(request.getEndpoint().getAddress(), url);
    }

    @Test
    public void testOverrideOfAllValues() throws Exception {
        MessageId messageId = new MessageId("messageid");
        TransmissionRequest request = transmissionRequestBuilder
                .payLoad(inputStreamWithSBDH)
                .sender(WellKnownParticipant.DIFI_TEST)
                .receiver(WellKnownParticipant.U4_TEST)
                .documentType(PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier())
                .processType(PeppolProcessTypeIdAcronym.ORDER_ONLY.getPeppolProcessTypeId())
                .build();

        Header header = request.getHeader();
        assertEquals(header.getSender(), WellKnownParticipant.DIFI_TEST.toVefa());
        assertEquals(header.getReceiver(), WellKnownParticipant.U4_TEST.toVefa());
        assertEquals(header.getDocumentType(), PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier().toVefa());
        assertEquals(header.getProcess(), PeppolProcessTypeIdAcronym.ORDER_ONLY.getPeppolProcessTypeId().toVefa());
        assertNotEquals(header.getIdentifier().getValue(), messageId.stringValue(),
                "The SBDH instanceId should not be equal to the AS2 MessageId");
    }

    /**
     * If a messageId is not provided a default one is created before sending.
     */
    @Test
    public void testMessageIdSuppliedByBuilder() throws OxalisException {
        TransmissionRequest request = transmissionRequestBuilder
                .payLoad(inputStreamWithSBDH)
                .sender(WellKnownParticipant.DIFI_TEST)
                .receiver(WellKnownParticipant.U4_TEST)
                .documentType(PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier())
                .processType(PeppolProcessTypeIdAcronym.ORDER_ONLY.getPeppolProcessTypeId())
                .build();

        assertNotNull(request.getHeader().getIdentifier());

        transmissionRequestBuilder.reset();
        TransmissionRequest request2 = transmissionRequestBuilder.payLoad(noSbdhInputStream)
                .sender(WellKnownParticipant.DIFI_TEST)
                .receiver(WellKnownParticipant.U4_TEST)
                .documentType(PeppolDocumentTypeIdAcronym.ORDER.getDocumentTypeIdentifier())
                .processType(PeppolProcessTypeIdAcronym.ORDER_ONLY.getPeppolProcessTypeId())
                .build();

        assertNotNull(request2.getHeader().getIdentifier());
    }

    @Test
    public void makeSureWeDetectMissingProperties() {
        try {
            transmissionRequestBuilder
                    .payLoad(missingMetadataInputStream)
                    .build();
            fail("The build() should have failed indicating missing properties");
        } catch (Exception ex) {
            assertEquals(ex.getMessage(), "TransmissionRequest can not be built, missing [recipientId, senderId] metadata.");
        }
    }

    @Test
    public void testIssue250() throws Exception {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("Issue250-sample-invoice.xml");
        assertNotNull(resourceAsStream);

        transmissionRequestBuilder.reset();
        TransmissionRequest transmissionRequest = transmissionRequestBuilder.payLoad(resourceAsStream).build();

        ParticipantIdentifier recipientId = transmissionRequest.getHeader().getReceiver();
        assertEquals(recipientId, new ParticipantId("9954:111111111").toVefa());
        assertNotNull(transmissionRequest.getHeader().getIdentifier());
    }
}