package no.nav.foreldrepenger.domene.person.pdl;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.pdl.FalskIdentitetIdentifiserendeInformasjonResponseProjection;
import no.nav.pdl.FalskIdentitetResponseProjection;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PersonnavnResponseProjection;
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

    // Man kan tenke seg å hente rett ident eller navn/statsborgerskap fra falskIdentitet-informasjonen. Se an frekvens og innhold
    public void sjekkPersonFalskIdentitet(FagsakYtelseType ytelseType, AktørId aktørId, List<Folkeregisteridentifikator> folkeregisteridentifikatorer) {
        var identer = Optional.ofNullable(folkeregisteridentifikatorer).orElseGet(List::of).stream()
            .map(Folkeregisteridentifikator::getIdentifikasjonsnummer)
            .filter(Objects::nonNull)
            .toList();
        if (!identer.isEmpty()) {
            return;
        }
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().status().identifikasjonsnummer())
            .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().status())
            .falskIdentitet(new FalskIdentitetResponseProjection().erFalsk().rettIdentitetErUkjent().rettIdentitetVedIdentifikasjonsnummer()
                .rettIdentitetVedOpplysninger(new FalskIdentitetIdentifiserendeInformasjonResponseProjection().kjoenn().foedselsdato()
                    .personnavn(new PersonnavnResponseProjection().fornavn().mellomnavn().etternavn()).statsborgerskap()));

        var falskIdentitetPerson = hentPerson(ytelseType, query, projection);

        if (falskIdentitetPerson.getFalskIdentitet() != null && falskIdentitetPerson.getFalskIdentitet().getErFalsk()) {
            // Skal mangler personidentifikator, ha opphørt personstatus og kanskje informasjon i falskIdentitet
            if (falskIdentitetPerson.getFolkeregisteridentifikator() != null) {
                var identifikatorer = falskIdentitetPerson.getFolkeregisteridentifikator().stream()
                    .map(p -> p.getIdentifikasjonsnummer().substring(p.getIdentifikasjonsnummer().length() - 5) + " " + p.getStatus())
                    .toList();
                if (!identifikatorer.isEmpty()) {
                    LOG.warn("Falsk identitet aktør {} har identer {}", aktørId, identifikatorer);
                }
            }
            if (falskIdentitetPerson.getFolkeregisterpersonstatus() != null) {
                var statuser = falskIdentitetPerson.getFolkeregisterpersonstatus().stream()
                    .map(Folkeregisterpersonstatus::getStatus)
                    .map(PersonstatusType::fraFregPersonstatus)
                    .toList();
                if (!statuser.contains(PersonstatusType.UTPE)) {
                    LOG.warn("Falsk identitet aktør {} har ikke status opphørt {}", aktørId, statuser);
                }
            }
            if (Objects.equals(falskIdentitetPerson.getFalskIdentitet().getRettIdentitetErUkjent(), Boolean.TRUE)) {
                LOG.warn("Falsk identitet aktør {} har rettIdentitetErUkjent", aktørId);
            } else if (falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedIdentifikasjonsnummer() != null) {
                LOG.warn("Falsk identitet aktør {} har rettIdentitetVedIdentifikasjonsnummer {}", aktørId, falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedIdentifikasjonsnummer());
            } else if (falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger() != null) {
                var kjønn = falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger().getKjoenn();
                var statsborgerskap = falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger().getStatsborgerskap();
                var fødselsdato = falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger().getFoedselsdato();
                var navn = Optional.ofNullable(falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger().getPersonnavn())
                    .map(p -> Optional.ofNullable(p.getFornavn()).orElse("UtenFN")
                        + leftPad(Optional.ofNullable(p.getMellomnavn()).orElse("UtenMN"))
                        + leftPad(Optional.ofNullable(p.getEtternavn()).map(e -> "ETTERN").orElse("UtenEN")))
                    .orElse("UtenNavn");
                LOG.warn("Falsk identitet aktør {} har rettIdentitetVedOpplysninger navn {} fdato {} kjønn {} statsborger {}", aktørId, navn, fødselsdato, kjønn, statsborgerskap);
            } else {
                LOG.warn("Falsk identitet aktør {} mangler info om rett identitet", aktørId);
            }
        } else {
            LOG.warn("Person uten folkeregisteridentifikator aktør {} - ikke falsk person", aktørId);
        }
    }

    private static String leftPad(String navn) {
        return Optional.ofNullable(navn).map(n -> " " + navn).orElse("");
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
