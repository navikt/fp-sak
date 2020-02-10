package no.nav.foreldrepenger.domene.arbeidsgiver;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class ArbeidsgiverTjenesteImpl implements ArbeidsgiverTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
    private static final long SHORT_CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private PersonIdentTjeneste tpsTjeneste;
    private LRUCache<String, ArbeidsgiverOpplysninger> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private LRUCache<String, ArbeidsgiverOpplysninger> failBackoffCache = new LRUCache<>(100, SHORT_CACHE_ELEMENT_LIVE_TIME_MS);
    private VirksomhetTjeneste virksomhetTjeneste;

    ArbeidsgiverTjenesteImpl() {
        // CDI
    }

    @Inject
    public ArbeidsgiverTjenesteImpl(PersonIdentTjeneste tpsTjeneste, VirksomhetTjeneste virksomhetTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    @Override
    public ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = cache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        arbeidsgiverOpplysninger = failBackoffCache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && !Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            String orgnr = arbeidsgiver.getOrgnr();
            var virksomhet = virksomhetTjeneste.hentOgLagreOrganisasjon(orgnr);
            ArbeidsgiverOpplysninger nyOpplysninger = new ArbeidsgiverOpplysninger(orgnr, virksomhet.getNavn());
            cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
            return nyOpplysninger;
        } else if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            return new ArbeidsgiverOpplysninger(OrgNummer.KUNSTIG_ORG, "Kunstig(Lagt til av saksbehandling)");
        } else if (arbeidsgiver.erAktørId()) {
            Optional<Personinfo> personinfo = hentInformasjonFraTps(arbeidsgiver);
            if (personinfo.isPresent()) {
                Personinfo info = personinfo.get();
                String fødselsdato = info.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                ArbeidsgiverOpplysninger nyOpplysninger = new ArbeidsgiverOpplysninger(fødselsdato, info.getNavn(), info.getFødselsdato());
                cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
                return nyOpplysninger;
            } else {
                // Putter bevist ikke denne i cache da denne aktøren ikke er kjent, men legger denne i en backoff cache som benyttes for at vi ikke skal hamre på tps ved sikkerhetsbegrensning
                ArbeidsgiverOpplysninger opplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), "N/A");
                failBackoffCache.put(arbeidsgiver.getIdentifikator(), opplysninger);
                return opplysninger;
            }
        }
        return null;
    }

    @Override
    public Virksomhet hentVirksomhet(String orgNummer) {
        return virksomhetTjeneste.hentVirksomhet(orgNummer).orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + orgNummer));
    }

    @Override
    public Arbeidsgiver hentArbeidsgiver(String orgnr, String arbeidsgiverIdentifikator) {
        if (orgnr != null) {
            return Arbeidsgiver.fra(hentVirksomhet(orgnr));
        }
        if (arbeidsgiverIdentifikator != null) {
            return Arbeidsgiver.fra(new AktørId(arbeidsgiverIdentifikator));
        }
        return null;
    }

    private Optional<Personinfo> hentInformasjonFraTps(Arbeidsgiver arbeidsgiver) {
        try {
            return tpsTjeneste.hentBrukerForAktør(arbeidsgiver.getAktørId());
        } catch (VLException feil) {
            // Ønsker ikke å gi GUI problemer ved å eksponere exceptions
            return Optional.empty();
        }
    }
}
