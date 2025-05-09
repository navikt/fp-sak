package no.nav.foreldrepenger.domene.person.pdl;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.SocketTimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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

    public Person hentPerson(FagsakYtelseType ytelseType, HentPersonQueryRequest q, PersonResponseProjection p) {
        try {
            var ytelse = utledYtelse(ytelseType);
            return pdlKlient.hentPerson(ytelse, q, p);
        } catch (PdlException e) {
            if (e.getStatus() == HTTP_NOT_FOUND) {
                LOG.info("PDL FPSAK hentPerson ikke funnet");
            } else {
                LOG.warn("PDL FPSAK hentPerson feil fra PDL pga {}", e, e);
            }
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }

    public Person hentPersonTilgangsnektSomInfo(FagsakYtelseType ytelseType, HentPersonQueryRequest q, PersonResponseProjection p) {
        try {
            var ytelse = utledYtelse(ytelseType);
            return pdlKlient.hentPerson(ytelse, q, p);
        } catch (PdlException e) {
            if (e.getStatus() == HTTP_NOT_FOUND) {
                LOG.info("PDL FPSAK hentPerson ikke funnet");
            } else {
                LOG.info("PDL FPSAK hentPerson feil fra PDL pga {}", e, e);
            }
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }

    public Person hentPerson(FagsakYtelseType ytelseType, HentPersonQueryRequest q, PersonResponseProjection p, boolean ignoreNotFound) {
        try {
            var ytelse = utledYtelse(ytelseType);
            return pdlKlient.hentPerson(ytelse, q, p, ignoreNotFound);
        } catch (PdlException e) {
            LOG.warn("PDL FPSAK hentPerson feil fra PDL pga {}", e, e);
            throw e;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException(PDL_TIMEOUT_KODE, PDL_TIMEOUT_MSG) : e;
        }
    }

    private static Persondata.Ytelse utledYtelse(FagsakYtelseType ytelseType) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return Persondata.Ytelse.ENGANGSSTØNAD;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return Persondata.Ytelse.SVANGERSKAPSPENGER;
        } else {
            return Persondata.Ytelse.FORELDREPENGER;
        }
    }
}
