package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

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
        if (!forrigeBeslutning || forrigeRes.getUtbetBeregningsresultatFP() == null) {
            // Ingenting å kopiere hvis vi ikke gjorde omfordeling sist
            return false;
        }

        var nyttForeslåttResultat = beregningsresultatRepository.hentBeregningsresultat(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + ref.getBehandlingId()));
        var forrigeForeslåttResultat = forrigeRes.getBgBeregningsresultatFP();

        // Hvis det foreslåtte resultatet er likt kan vi kopiere det utbetalte resultatet
        return SammenlignBeregningsresultat.erLike(nyttForeslåttResultat, forrigeForeslåttResultat);
    }

    public void kopierOgLagreUtbetBeregningsresultat(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var forrigeRes = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid)).orElseThrow();
        var forrigeUtbetResultat = forrigeRes.getUtbetBeregningsresultatFP();
        var skulleHindreTilbaketrekkIForrigeBehandling = forrigeRes.skalHindreTilbaketrekk().orElseThrow();

        // Lagre original beslutning
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, skulleHindreTilbaketrekkIForrigeBehandling);

        if (skulleHindreTilbaketrekkIForrigeBehandling) {

            // Kopier utbet til ny entitet
            var nyttUtbetResultat = lagKopi(forrigeUtbetResultat);

            var kopiErLikOriginal = SammenlignBeregningsresultat.erLike(nyttUtbetResultat, forrigeUtbetResultat);
            if (!kopiErLikOriginal) {
                throw new IllegalStateException("Gammelt og nytt utbetalt beregningsresultat er ikke likt for behandling " + ref.getBehandlingId());
            }

            // Beregn feriepenger
            var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
            feriepengerTjeneste.beregnFeriepenger(behandling, nyttUtbetResultat);

            // Lagre utbet entitet
            LOG.info("FP-587469: Lagrer kopiert utbetalt resultat på behandling med id " + ref.getBehandlingId() +
                " kopiert fra behandling med id " + ref.getOriginalBehandlingId().get());
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, nyttUtbetResultat);
        }
    }

    private BeregningsresultatEntitet lagKopi(BeregningsresultatEntitet forrigeUtbetResultat) {
        var nyttUtbetResultat = BeregningsresultatEntitet.builder()
            .medRegelSporing(forrigeUtbetResultat.getRegelSporing())
            .medRegelInput(forrigeUtbetResultat.getRegelInput())
            .medEndringsdato(forrigeUtbetResultat.getEndringsdato().orElse(null))
            .build();

        forrigeUtbetResultat.getBeregningsresultatPerioder().forEach(utbPeriode -> {
            var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(utbPeriode.getBeregningsresultatPeriodeFom(), utbPeriode.getBeregningsresultatPeriodeTom())
                .build(nyttUtbetResultat);
            utbPeriode.getBeregningsresultatAndelList().forEach(andel ->
                BeregningsresultatAndel.builder(Kopimaskin.deepCopy(andel))
                    .build(beregningsresultatPeriode)
            );
        });
        return nyttUtbetResultat;
    }
}
