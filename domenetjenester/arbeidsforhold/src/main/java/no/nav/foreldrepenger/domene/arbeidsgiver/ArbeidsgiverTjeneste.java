package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class ArbeidsgiverTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
    private static final long SHORT_CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private static final LRUCache<String, ArbeidsgiverOpplysninger> CACHE = new LRUCache<>(2500, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, ArbeidsgiverOpplysninger> FAIL_BACKOFF_CACHE = new LRUCache<>(100, SHORT_CACHE_ELEMENT_LIVE_TIME_MS);

    private PersoninfoAdapter personinfoAdapter;
    private VirksomhetTjeneste virksomhetTjeneste;

    ArbeidsgiverTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsgiverTjeneste(PersoninfoAdapter personinfoAdapter, VirksomhetTjeneste virksomhetTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        var arbeidsgiverOpplysninger = CACHE.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            CACHE.put(arbeidsgiver.getIdentifikator(), arbeidsgiverOpplysninger);
            return arbeidsgiverOpplysninger;
        }
        arbeidsgiverOpplysninger = FAIL_BACKOFF_CACHE.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && !Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            var orgnr = arbeidsgiver.getOrgnr();
            var virksomhet = virksomhetTjeneste.hentOrganisasjon(orgnr);
            var nyOpplysninger = new ArbeidsgiverOpplysninger(orgnr, virksomhet.getNavn());
            CACHE.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
            return nyOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            return new ArbeidsgiverOpplysninger(OrgNummer.KUNSTIG_ORG, "Kunstig(Lagt til av saksbehandling)");
        }
        if (arbeidsgiver.erAktørId()) {
            var personinfo = hentInformasjonFraPDL(arbeidsgiver);
            if (personinfo.isPresent()) {
                var info = personinfo.get();
                var fødselsdato = info.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                var nyOpplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getAktørId(), fødselsdato, info.getNavn(),
                        info.getFødselsdato());
                CACHE.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
                return nyOpplysninger;
            } else {
                // Putter bevist ikke denne i cache da denne aktøren ikke er kjent, men legger
                // denne i en backoff cache som benyttes for at vi ikke skal hamre på pdl ved
                // sikkerhetsbegrensning
                var opplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), "N/A");
                FAIL_BACKOFF_CACHE.put(arbeidsgiver.getIdentifikator(), opplysninger);
                return opplysninger;
            }
        }
        return null;
    }

    public Virksomhet hentVirksomhet(String orgNummer) {
        return virksomhetTjeneste.finnOrganisasjon(orgNummer)
                .orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + orgNummer));
    }

    private Optional<PersoninfoArbeidsgiver> hentInformasjonFraPDL(Arbeidsgiver arbeidsgiver) {
        try {
            return personinfoAdapter.hentBrukerArbeidsgiverForAktør(arbeidsgiver.getAktørId());
        } catch (Exception feil) {
            // Ønsker ikke å gi GUI problemer ved å eksponere exceptions
            return Optional.empty();
        }
    }
}
