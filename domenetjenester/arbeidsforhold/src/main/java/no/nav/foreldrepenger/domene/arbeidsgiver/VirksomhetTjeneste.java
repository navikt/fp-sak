package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetAlleredeLagretException;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.domene.arbeidsgiver.rest.EregOrganisasjonRestKlient;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.JuridiskEnhet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;
import no.nav.vedtak.felles.integrasjon.organisasjon.hent.HentOrganisasjonRequest;
import no.nav.vedtak.util.env.Cluster;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class VirksomhetTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirksomhetTjeneste.class);
    private static final String TJENESTE = "Organisasjon";
    private OrganisasjonConsumer organisasjonConsumer;
    private EregOrganisasjonRestKlient eregRestKlient;
    private VirksomhetRepository virksomhetRepository;
    private boolean isProd = Cluster.PROD_FSS.equals(Environment.current().getCluster());

    public VirksomhetTjeneste() {
        // CDI
    }

    @Inject
    public VirksomhetTjeneste(OrganisasjonConsumer organisasjonConsumer,
                              EregOrganisasjonRestKlient eregRestKlient,
                                  VirksomhetRepository virksomhetRepository) {
        this.organisasjonConsumer = organisasjonConsumer;
        this.virksomhetRepository = virksomhetRepository;
        this.eregRestKlient = eregRestKlient;
    }

    public VirksomhetTjeneste(OrganisasjonConsumer organisasjonConsumer,
                              VirksomhetRepository virksomhetRepository) {
        this.organisasjonConsumer = organisasjonConsumer;
        this.virksomhetRepository = virksomhetRepository;
    }

    /**
     * Henter informasjon fra Enhetsregisteret hvis applikasjonen ikke kjenner til orgnr eller har data som er eldre enn 24 timer.
     *
     * @param orgNummer orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i enhetsreg
     */
    public Virksomhet hentOgLagreOrganisasjon(String orgNummer) {
        final Optional<Virksomhet> virksomhetOptional = virksomhetRepository.hent(orgNummer);
        if (virksomhetOptional.isEmpty() || virksomhetOptional.get().skalRehentes()) {
            HentOrganisasjonResponse response = hentOrganisasjon(orgNummer);
            final Virksomhet virksomhet = mapOrganisasjonResponseToOrganisasjon(response.getOrganisasjon(), virksomhetOptional);
            if (isProd)
                sammenlignLoggRest(orgNummer, (VirksomhetEntitet)virksomhet);
            return lagreVirksomhet(virksomhetOptional, virksomhet);
        }
        return virksomhetOptional.orElseThrow(() -> new IllegalArgumentException("Fant ikke virksomhet for orgNummer=" + orgNummer));
    }

    public Optional<Virksomhet> hentVirksomhet(String orgNummer) {
        if (orgNummer == null)
            return Optional.empty();
        if(OrgNummer.erKunstig(orgNummer)) {
            return virksomhetRepository.hent(orgNummer);
        }
        // forsøker å hente/lagre uansett om orgnummer er gyldig eller ikke.
        return OrganisasjonsNummerValidator.erGyldig(orgNummer) ? Optional.of(hentOgLagreOrganisasjon(orgNummer)) : Optional.empty();
    }

    public Optional<Virksomhet> finnOrganisasjon(String orgNummer) {
        if (orgNummer == null)
            return Optional.empty();
        if(OrgNummer.erKunstig(orgNummer)) {
            return virksomhetRepository.hent(orgNummer);
        }
        // etter endring til abakus må vi uansett forsøke å hente på nytt
        return OrganisasjonsNummerValidator.erGyldig(orgNummer) ? Optional.of(hentOgLagreOrganisasjon(orgNummer)) : Optional.empty();
    }

    private Virksomhet lagreVirksomhet(Optional<Virksomhet> virksomhetOptional, Virksomhet virksomhet) {
        try {
            virksomhetRepository.lagre(virksomhet);
            return virksomhet;
        } catch (VirksomhetAlleredeLagretException exception) {
            return virksomhetOptional.orElseThrow(IllegalStateException::new);
        }
    }

    private HentOrganisasjonResponse hentOrganisasjon(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer"); // NOSONAR
        HentOrganisasjonRequest request = new HentOrganisasjonRequest(orgNummer);
        try {
            return organisasjonConsumer.hentOrganisasjon(request);
        } catch (HentOrganisasjonOrganisasjonIkkeFunnet e) {
            throw OrganisasjonTjenesteFeil.FACTORY.organisasjonIkkeFunnet(orgNummer, e).toException();
        } catch (HentOrganisasjonUgyldigInput e) {
            throw OrganisasjonTjenesteFeil.FACTORY.ugyldigInput(TJENESTE, orgNummer, e).toException();
        }
    }

    private Virksomhet mapOrganisasjonResponseToOrganisasjon(Organisasjon responsOrganisasjon, Optional<Virksomhet> virksomhetOptional) {
        var builder = getBuilder(virksomhetOptional)
            .medNavn(((UstrukturertNavn) responsOrganisasjon.getNavn()).getNavnelinje().stream().filter(it -> !it.isEmpty())
                .reduce("", (a, b) -> a + " " + b).trim())
            .medRegistrert(DateUtil.convertToLocalDate(responsOrganisasjon.getOrganisasjonDetaljer().getRegistreringsDato()));
        if (!virksomhetOptional.isPresent()) {
            builder.medOrgnr(responsOrganisasjon.getOrgnummer());
        }

        if (responsOrganisasjon instanceof no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Virksomhet) {
            no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Virksomhet virksomhet = (no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Virksomhet) responsOrganisasjon;

            if (virksomhet.getVirksomhetDetaljer().getOppstartsdato() != null) {
                builder.medOppstart(DateUtil.convertToLocalDate(virksomhet.getVirksomhetDetaljer().getOppstartsdato()));
            }
            if (virksomhet.getVirksomhetDetaljer().getNedleggelsesdato() != null) {
                builder.medAvsluttet(DateUtil.convertToLocalDate(virksomhet.getVirksomhetDetaljer().getNedleggelsesdato()));
            }
            builder.medOrganisasjonstype(Organisasjonstype.VIRKSOMHET);
        } else if (responsOrganisasjon instanceof JuridiskEnhet) {
            builder.medOrganisasjonstype(Organisasjonstype.JURIDISK_ENHET);
        } else {
            // OrgLedd
            //
        }
        return builder.oppdatertOpplysningerNå().build();
    }

    private void sammenlignLoggRest(String orgNummer, VirksomhetEntitet virksomhet) {
        try {
            var org = eregRestKlient.hentOrganisasjon(orgNummer);
            var builder = getBuilder(Optional.empty())
                .medNavn(org.getNavn())
                .medRegistrert(org.getRegistreringsdato())
                .medOrgnr(org.getOrganisasjonsnummer());
            if ("Virksomhet".equalsIgnoreCase(org.getType())) {
                builder.medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
                    .medOppstart(org.getOppstartsdato())
                    .medAvsluttet(org.getNedleggelsesdato());
            } else if ("JuridiskEnhet".equalsIgnoreCase(org.getType())) {
                builder.medOrganisasjonstype(Organisasjonstype.JURIDISK_ENHET);
            }
            var rest = builder.build();
            if (virksomhet.erLik(rest)) {
                LOGGER.info("FPSAK EREG REST likt svar");
            } else {
                LOGGER.info("FPSAK EREG REST avvik WS {} RS {}", virksomhet.tilString(), rest.tilString());
            }
        } catch (Exception e) {
            LOGGER.info("FPSAK EREG REST noe gikk feil", e);
        }
    }

    private VirksomhetEntitet.Builder getBuilder(Optional<Virksomhet> virksomhetOptional) {
        return virksomhetOptional.map(VirksomhetEntitet.Builder::new).orElseGet(VirksomhetEntitet.Builder::new);
    }

}
