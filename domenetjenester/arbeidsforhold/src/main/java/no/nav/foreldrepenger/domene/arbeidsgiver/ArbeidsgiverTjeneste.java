package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class ArbeidsgiverTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
    private static final long SHORT_CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private PersonIdentTjeneste tpsTjeneste;
    private LRUCache<String, ArbeidsgiverOpplysninger> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private LRUCache<String, ArbeidsgiverOpplysninger> failBackoffCache = new LRUCache<>(100, SHORT_CACHE_ELEMENT_LIVE_TIME_MS);
    private VirksomhetTjeneste virksomhetTjeneste;

    ArbeidsgiverTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsgiverTjeneste(PersonIdentTjeneste tpsTjeneste, VirksomhetTjeneste virksomhetTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        var arbeidsgiverOpplysninger = cache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        arbeidsgiverOpplysninger = failBackoffCache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && !Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            var orgnr = arbeidsgiver.getOrgnr();
            var virksomhet = virksomhetTjeneste.hentOrganisasjon(orgnr);
            var nyOpplysninger = new ArbeidsgiverOpplysninger(orgnr, virksomhet.getNavn());
            cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
            return nyOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            return new ArbeidsgiverOpplysninger(OrgNummer.KUNSTIG_ORG, "Kunstig(Lagt til av saksbehandling)");
        }
        if (arbeidsgiver.erAktørId()) {
            var personinfo = hentInformasjonFraTps(arbeidsgiver);
            if (personinfo.isPresent()) {
                var info = personinfo.get();
                var fødselsdato = info.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                var nyOpplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getAktørId(), fødselsdato, info.getNavn(),
                        info.getFødselsdato());
                cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
                return nyOpplysninger;
            }
            // Putter bevist ikke denne i cache da denne aktøren ikke er kjent, men legger
            // denne i en backoff cache som benyttes for at vi ikke skal hamre på tps ved
            // sikkerhetsbegrensning
            var opplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), "N/A");
            failBackoffCache.put(arbeidsgiver.getIdentifikator(), opplysninger);
            return opplysninger;
        }
        return null;
    }

    public Virksomhet hentVirksomhet(String orgNummer) {
        return virksomhetTjeneste.finnOrganisasjon(orgNummer)
                .orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + orgNummer));
    }

    private Optional<PersoninfoArbeidsgiver> hentInformasjonFraTps(Arbeidsgiver arbeidsgiver) {
        try {
            return tpsTjeneste.hentBrukerForAktør(arbeidsgiver.getAktørId());
        } catch (VLException feil) {
            // Ønsker ikke å gi GUI problemer ved å eksponere exceptions
            return Optional.empty();
        }
    }
}
