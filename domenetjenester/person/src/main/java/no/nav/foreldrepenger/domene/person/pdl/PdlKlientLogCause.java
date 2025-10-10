package no.nav.foreldrepenger.domene.person.pdl;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoFalskIdentitet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.FalskIdentitetIdentifiserendeInformasjonResponseProjection;
import no.nav.pdl.FalskIdentitetResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PersonnavnResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.person.PdlException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.konfig.Tid;

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
    public PersoninfoFalskIdentitet sjekkUtenIdentifikatorFalskIdentitet(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().status())
                .falskIdentitet(new FalskIdentitetResponseProjection().erFalsk().rettIdentitetErUkjent().rettIdentitetVedIdentifikasjonsnummer()
                    .rettIdentitetVedOpplysninger(new FalskIdentitetIdentifiserendeInformasjonResponseProjection().kjoenn().foedselsdato()
                        .personnavn(new PersonnavnResponseProjection().fornavn().mellomnavn().etternavn()).statsborgerskap()));

            var falskIdentitetPerson = hentPerson(ytelseType, query, projection);
            loggUtenIdentifikatorFalskIdentitet(falskIdentitetPerson, aktørId);
            return lagPersonFalskIdentitet(falskIdentitetPerson, aktørId, personIdent);
        } catch (Exception e) {
            LOG.warn("Person uten folkeregisteridentifikator aktør {} - fikk feil ved oppslag falsk identitet", aktørId);
            return  null;
        }
    }

    public void loggUtenIdentifikatorFalskIdentitet(Person falskIdentitetPerson, AktørId aktørId) {
        if (falskIdentitetPerson.getFalskIdentitet() != null && falskIdentitetPerson.getFalskIdentitet().getErFalsk()) {
            // Falsk Identitet skal mangle personidentifikator, ha opphørt personstatus og kanskje informasjon i falskIdentitet
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
                var rettIdentitet = falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger();
                var kjønn = rettIdentitet.getKjoenn();
                var statsborgerskap = rettIdentitet.getStatsborgerskap();
                var fødselsdato = rettIdentitet.getFoedselsdato();
                var navn = rettIdentitet.getPersonnavn().getFornavn()
                        + PersonMappers.leftPad(Optional.ofNullable(rettIdentitet.getPersonnavn().getMellomnavn()).orElse("UtenMN"))
                        + "ETTERN";
                LOG.warn("Falsk identitet aktør {} har rettIdentitetVedOpplysninger navn {} fdato {} kjønn {} statsborger {}", aktørId, navn, fødselsdato, kjønn, statsborgerskap);
            } else {
                LOG.warn("Falsk identitet aktør {} mangler info om rett identitet", aktørId);
            }
        } else {
            // Tilfelle som ikke er falsk identitet
            LOG.warn("Person uten folkeregisteridentifikator aktør {} - ikke falsk person", aktørId);
        }
    }

    public PersoninfoFalskIdentitet lagPersonFalskIdentitet(Person falskIdentitetPerson, AktørId aktørId, PersonIdent personIdent) {
        if (falskIdentitetPerson.getFalskIdentitet() != null && falskIdentitetPerson.getFalskIdentitet().getErFalsk()) {
            // Falsk Identitet skal mangle personidentifikator, ha opphørt personstatus og kanskje informasjon i falskIdentitet
            if (Objects.equals(falskIdentitetPerson.getFalskIdentitet().getRettIdentitetErUkjent(), Boolean.TRUE)) {
                return new PersoninfoFalskIdentitet(aktørId, personIdent, "Falsk Identitet", Tid.TIDENES_BEGYNNELSE, NavBrukerKjønn.UDEFINERT, Landkoder.XUK, PersonstatusType.UTPE);
            } else if (falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedIdentifikasjonsnummer() != null) {
                throw new IllegalStateException("Falsk identitet: rettIdentitetVedIdentifikasjonsnummer finnes");
            } else if (falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger() != null) {
                var rettIdentitet = falskIdentitetPerson.getFalskIdentitet().getRettIdentitetVedOpplysninger();
                var kjønn = Optional.ofNullable(rettIdentitet.getKjoenn()).map(PersonMappers::mapKjønn).orElse(NavBrukerKjønn.UDEFINERT);
                var navn = PersonMappers.mapNavn(rettIdentitet.getPersonnavn());
                var fødselsdato = Optional.ofNullable(rettIdentitet.getFoedselsdato()).map(PersonMappers::mapDato).orElse(Tid.TIDENES_BEGYNNELSE);
                var statsborgerskap = rettIdentitet.getStatsborgerskap().stream()
                    .map(Landkoder::fraKodeDefaultUdefinert)
                    .filter(l -> !Landkoder.UDEFINERT.equals(l) && !Landkoder.XUK.equals(l))
                    .findFirst().orElse(Landkoder.XUK);
                return new PersoninfoFalskIdentitet(aktørId, personIdent, navn, fødselsdato, kjønn, statsborgerskap, PersonstatusType.UTPE);
            } else {
                throw new IllegalStateException("Falsk identitet: mangler delopplysning");
            }
        } else {
            return null;
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
