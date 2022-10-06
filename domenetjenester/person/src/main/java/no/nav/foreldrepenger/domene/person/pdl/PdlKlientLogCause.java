package no.nav.foreldrepenger.domene.person.pdl;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.SocketTimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.pdl.GeografiskTilknytning;
import no.nav.pdl.GeografiskTilknytningResponseProjection;
import no.nav.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.person.PdlException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

@ApplicationScoped
public class PdlKlientLogCause {

    private static final String PDL_TIMEOUT_KODE = "FP-723618";
    private static final String PDL_TIMEOUT_MSG = "PDL timeout";
    private static final Logger LOG = LoggerFactory.getLogger(PdlKlientLogCause.class);

    private Persondata pdlKlient;

    PdlKlientLogCause() {
        // CDI
    }

    @Inject
    public PdlKlientLogCause(Persondata pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public GeografiskTilknytning hentGT(HentGeografiskTilknytningQueryRequest q, GeografiskTilknytningResponseProjection p) {
        try {
            return pdlKlient.hentGT(q, p);
        } catch (PdlException e) {
            if (e.getStatus() == HTTP_NOT_FOUND) {
                LOG.info("PDL FPSAK hentGT person ikke funnet");
            } else {
                LOG.warn("PDL FPSAK hentGT feil fra PDL pga {}", e.toString(), e);
            }
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }

    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p) {
        try {
            return pdlKlient.hentPerson(q, p);
        } catch (PdlException e) {
            if (e.getStatus() == HTTP_NOT_FOUND) {
                LOG.info("PDL FPSAK hentPerson ikke funnet");
            } else {
                LOG.warn("PDL FPSAK hentPerson feil fra PDL pga {}", e.toString(), e);
            }
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }

    public Person hentPerson(HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound) {
        try {
            return pdlKlient.hentPerson(q, p, ignoreNotFound);
        } catch (PdlException e) {
            if (e.getStatus() == HTTP_NOT_FOUND) {
                LOG.info("PDL FPSAK hentPerson ikke funnet");
            } else if (e.getStatus() != HTTP_NOT_FOUND) {
                LOG.warn("PDL FPSAK hentPerson feil fra PDL pga {}", e.toString(), e);
            }
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }
}
