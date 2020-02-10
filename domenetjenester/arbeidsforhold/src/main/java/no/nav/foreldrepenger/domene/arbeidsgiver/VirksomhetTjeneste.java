package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetAlleredeLagretException;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.JuridiskEnhet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;
import no.nav.vedtak.felles.integrasjon.organisasjon.hent.HentOrganisasjonRequest;

@ApplicationScoped
public class VirksomhetTjeneste {

    private static final String TJENESTE = "Organisasjon";
    private OrganisasjonConsumer organisasjonConsumer;
    private VirksomhetRepository virksomhetRepository;

    public VirksomhetTjeneste() {
        // CDI
    }

    @Inject
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
            return lagreVirksomhet(virksomhetOptional, response);
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

    private Virksomhet lagreVirksomhet(Optional<Virksomhet> virksomhetOptional, HentOrganisasjonResponse response) {
        final Virksomhet virksomhet = mapOrganisasjonResponseToOrganisasjon(response.getOrganisasjon(), virksomhetOptional);
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

    private VirksomhetEntitet.Builder getBuilder(Optional<Virksomhet> virksomhetOptional) {
        return virksomhetOptional.map(VirksomhetEntitet.Builder::new).orElseGet(VirksomhetEntitet.Builder::new);
    }

}
