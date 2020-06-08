package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.rest.OrganisasjonRestKlient;
import no.nav.foreldrepenger.domene.arbeidsgiver.rest.OrganisasjonstypeEReg;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class VirksomhetTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final Virksomhet KUNSTIG_VIRKSOMHET = new Virksomhet.Builder()
        .medNavn("Kunstig virksomhet")
        .medOrganisasjonstype(Organisasjonstype.KUNSTIG)
        .medOrgnr(OrgNummer.KUNSTIG_ORG)
        .medRegistrert(LocalDate.of(1978, 01, 01))
        .medOppstart(LocalDate.of(1978, 01, 01))
        .build();

    private LRUCache<String, Virksomhet> cache = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private OrganisasjonRestKlient eregRestKlient;

    public VirksomhetTjeneste() {
        // CDI
    }

    @Inject
    public VirksomhetTjeneste(OrganisasjonRestKlient eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    /**
     * Henter informasjon fra Enhetsregisteret hvis applikasjonen ikke kjenner til orgnr eller har data som er eldre enn 24 timer.
     *
     * @param orgNummer orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i enhetsreg
     */
    public Virksomhet hentOgLagreOrganisasjon(String orgNummer) {
        final Optional<Virksomhet> virksomhetOptional = hent(orgNummer);
        if (virksomhetOptional.isEmpty()) {
            final Virksomhet virksomhet = hentOrganisasjonRest(orgNummer);
            lagre(virksomhet);
            return virksomhet;
        }
        return virksomhetOptional.orElseThrow(() -> new IllegalArgumentException("Fant ikke virksomhet for orgNummer=" + orgNummer));
    }

    public Optional<Virksomhet> finnOrganisasjon(String orgNummer) {
        if (orgNummer == null)
            return Optional.empty();
        if(OrgNummer.erKunstig(orgNummer)) {
            return hent(orgNummer);
        }
        return OrganisasjonsNummerValidator.erGyldig(orgNummer) ? Optional.of(hentOgLagreOrganisasjon(orgNummer)) : Optional.empty();
    }

    private Virksomhet hentOrganisasjonRest(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer"); // NOSONAR
        var org = eregRestKlient.hentOrganisasjon(orgNummer);
        var builder = Virksomhet.getBuilder()
            .medNavn(org.getNavn())
            .medRegistrert(org.getRegistreringsdato())
            .medOrgnr(org.getOrganisasjonsnummer());
        if (OrganisasjonstypeEReg.VIRKSOMHET.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
                .medOppstart(org.getOppstartsdato())
                .medAvsluttet(org.getNedleggelsesdato());
        } else if (OrganisasjonstypeEReg.JURIDISK_ENHET.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.JURIDISK_ENHET);
        } else if (OrganisasjonstypeEReg.ORGLEDD.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.ORGLEDD);
        }
        return builder.build();
    }

    private Optional<Virksomhet> hent(String orgnr) {
        if (Objects.equals(KUNSTIG_VIRKSOMHET.getOrgnr(), orgnr)) {
            return Optional.of(KUNSTIG_VIRKSOMHET);
        }
        return Optional.ofNullable(cache.get(orgnr));
    }

    private void lagre(Virksomhet virksomhet) {
        cache.put(virksomhet.getOrgnr(), virksomhet);
    }



}
