package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;

@ApplicationScoped
public class KopierUtbetResultatTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KopierUtbetResultatTjeneste.class);

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;

    public KopierUtbetResultatTjeneste() {
        // CDI
    }

    @Inject
    public KopierUtbetResultatTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                       BehandlingRepository behandlingRepository,
                                       @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;

    }

    public boolean kanKopiereForrigeUtbetResultat(BehandlingReferanse ref) {
        var forrigeResOpt = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid));

        if (forrigeResOpt.isEmpty()) {
            return false;
        }

        var forrigeRes = forrigeResOpt.get();
        var forrigeBeslutning = forrigeRes.skalHindreTilbaketrekk().orElse(false);
        if (!forrigeBeslutning || forrigeRes.getUtbetBeregningsresultatFP().isEmpty()) {
            // Ingenting å kopiere hvis vi ikke gjorde omfordeling sist
            return false;
        }

        var nyttForeslåttResultat = beregningsresultatRepository.hentBeregningsresultat(ref.behandlingId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + ref.behandlingId()));
        var forrigeForeslåttResultat = forrigeRes.getBgBeregningsresultatFP();

        // Hvis det foreslåtte resultatet er likt kan vi kopiere det utbetalte resultatet
        return SammenlignBeregningsresultat.erLike(nyttForeslåttResultat, forrigeForeslåttResultat);
    }

    public void kopierOgLagreUtbetBeregningsresultat(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var forrigeRes = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid)).orElseThrow();
        var forrigeUtbetResultat = forrigeRes.getUtbetBeregningsresultatFP().orElseThrow(); // Har vært validert i kanKopiereForrige
        var skulleHindreTilbaketrekkIForrigeBehandling = forrigeRes.skalHindreTilbaketrekk().orElseThrow();

        // Lagre original beslutning
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, skulleHindreTilbaketrekkIForrigeBehandling);

        if (skulleHindreTilbaketrekkIForrigeBehandling) {

            // Kopier utbet til ny entitet
            var nyttUtbetResultat = lagKopi(forrigeUtbetResultat);

            var kopiErLikOriginal = SammenlignBeregningsresultat.erLike(nyttUtbetResultat, forrigeUtbetResultat);
            if (!kopiErLikOriginal) {
                throw new IllegalStateException("Gammelt og nytt utbetalt beregningsresultat er ikke likt for behandling " + ref.behandlingId());
            }

            // Beregn feriepenger
            var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.fagsakYtelseType()).orElseThrow();
            var feriepenger = feriepengerTjeneste.beregnFeriepenger(ref, nyttUtbetResultat);

            // Lagre utbet entitet
            LOG.info("FP-587469: Lagrer kopiert utbetalt resultat på behandling med id {} kopiert fra behandling med id {}", ref.behandlingId(),
                ref.getOriginalBehandlingId().orElse(0L));
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, nyttUtbetResultat, feriepenger);
        }
    }

    private BeregningsresultatEntitet lagKopi(BeregningsresultatEntitet forrigeUtbetResultat) {
        var nyttUtbetResultat = BeregningsresultatEntitet.builder()
            .medRegelSporing(forrigeUtbetResultat.getRegelSporing())
            .medRegelInput(forrigeUtbetResultat.getRegelInput())
            .medRegelVersjon(forrigeUtbetResultat.getRegelVersjon())
            .build();

        forrigeUtbetResultat.getBeregningsresultatPerioder().forEach(utbPeriode -> {
            var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(utbPeriode.getBeregningsresultatPeriodeFom(), utbPeriode.getBeregningsresultatPeriodeTom())
                .build(nyttUtbetResultat);
            utbPeriode.getBeregningsresultatAndelList().forEach(andel ->
                BeregningsresultatAndel.builder()
                    .medDagsats(andel.getDagsats())
                    .medDagsatsFraBg(andel.getDagsatsFraBg())
                    .medUtbetalingsgrad(andel.getUtbetalingsgrad())
                    .medStillingsprosent(andel.getStillingsprosent())
                    .medBrukerErMottaker(andel.erBrukerMottaker())
                    .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
                    .medArbeidsforholdRef(andel.getArbeidsforholdRef())
                    .medArbeidsforholdType(andel.getArbeidsforholdType())
                    .medAktivitetStatus(andel.getAktivitetStatus())
                    .medInntektskategori(andel.getInntektskategori())
                    .build(beregningsresultatPeriode)
            );
        });
        return nyttUtbetResultat;
    }
}
