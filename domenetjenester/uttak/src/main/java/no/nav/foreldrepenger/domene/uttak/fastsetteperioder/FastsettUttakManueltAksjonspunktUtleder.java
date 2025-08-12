package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.Spesialnummer;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class FastsettUttakManueltAksjonspunktUtleder {

    private static final Environment ENV = Environment.current();
    private static final Arbeidsgiver STORTINGET = Arbeidsgiver.virksomhet(Spesialnummer.STORTINGET.getOrgnummer());

    private FpUttakRepository fpUttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    FastsettUttakManueltAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    FastsettUttakManueltAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();

        List<AksjonspunktDefinisjon> aksjonspunkter = new ArrayList<>();

        utledAksjonspunktForManuellBehandlingFraRegler(behandlingId).ifPresent(aksjonspunkter::add);
        utledAksjonspunktForStortingsrepresentant(input).ifPresent(aksjonspunkter::add);
        if (ENV.isProd()) { // TODO (TFP-6302): Fjern etter test. Skal ikke være nødvendig å utlede følgende aksjonpunkt lenger
            utledAksjonspunktForAnnenpartEØS(behandlingId).ifPresent(aksjonspunkter::add);
        }

        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT)
            || input.harBehandlingÅrsak(BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE);
        }
        var fpGrunnlag = (ForeldrepengerGrunnlag) input.getYtelsespesifiktGrunnlag();
        if (fpGrunnlag.isDødsfall()) {
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
        }
        if (input.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);
        }
        return aksjonspunkter.stream().distinct().toList();
    }

    private Optional<AksjonspunktDefinisjon> utledAksjonspunktForManuellBehandlingFraRegler(Long behandlingId) {
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).orElseThrow();
        for (var periode : uttakResultat.getGjeldendePerioder().getPerioder()) {
            if (periode.getResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING)){
                return Optional.of(fastsettUttakAksjonspunkt());
            }
        }
        return Optional.empty();
    }

    private Optional<AksjonspunktDefinisjon> utledAksjonspunktForStortingsrepresentant(UttakInput input) {
        var intervall = uttaksIntervall(input.getBehandlingReferanse().behandlingId()).orElse(null);
        if (intervall == null) {
            return Optional.empty();
        }
        var aktørArbeid = input.getIayGrunnlag().getAktørArbeidFraRegister(input.getBehandlingReferanse().aktørId());
        var filter = new YrkesaktivitetFilter(aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of()));
        var representantVedUttak = filter.getFrilansOppdrag().stream()
            .filter(ya -> STORTINGET.equals(ya.getArbeidsgiver()))
            .anyMatch(ya -> filter.getAnsettelsesPerioderFrilans(ya).stream().map(AktivitetsAvtale::getPeriode).anyMatch(intervall::overlapper));

        // TODO: Vurder å sjekke input sione beregningsgrunnlagStatuser - så ikke avgåtte får AP. Men ett tilfelle har "ansettelse" til 30/9-21
        // TODO: Avklar om hvordan tilkommet Stortingsrepresentant skal håndteres og om de er synlig i beregning.
        if (representantVedUttak) {
            return Optional.of(AksjonspunktDefinisjon.FASTSETT_UTTAK_STORTINGSREPRESENTANT);
        }
        return Optional.empty();
    }

    private Optional<AksjonspunktDefinisjon> utledAksjonspunktForAnnenpartEØS(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .filter(YtelseFordelingAggregat::avklartAnnenForelderHarRettEØS)
            .map(yfa -> AksjonspunktDefinisjon.KONTROLLER_ANNENPART_EØS);
    }

    private AksjonspunktDefinisjon fastsettUttakAksjonspunkt() {
        return AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER;
    }

    private Optional<DatoIntervallEntitet> uttaksIntervall(Long behandlingId) {
        var uttakPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder).map(UttakResultatPerioderEntitet::getPerioder).orElse(List.of());
        var min = uttakPerioder.stream().map(UttakResultatPeriodeEntitet::getFom).min(Comparator.naturalOrder());
        var max = uttakPerioder.stream().map(UttakResultatPeriodeEntitet::getTom).max(Comparator.naturalOrder());
        return min.map(fom -> DatoIntervallEntitet.fraOgMedTilOgMed(fom, max.orElse(Tid.TIDENES_ENDE)));
    }

}
