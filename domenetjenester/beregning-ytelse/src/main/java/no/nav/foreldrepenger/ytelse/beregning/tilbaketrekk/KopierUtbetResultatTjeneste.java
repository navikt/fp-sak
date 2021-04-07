package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
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
import java.util.Optional;

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

    public boolean kanKopiereUtbetResultat(BehandlingReferanse ref) {
        Optional<BehandlingBeregningsresultatEntitet> forrigeResOpt = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid));

        if (forrigeResOpt.isEmpty()) {
            return false;
        }

        BehandlingBeregningsresultatEntitet forrigeRes = forrigeResOpt.get();
        Boolean forrigeBeslutning = forrigeRes.skalHindreTilbaketrekk().orElse(false);
        if (!forrigeBeslutning || forrigeRes.getUtbetBeregningsresultatFP() == null) {
            // Ingenting å kopiere
            return false;
        }

        BeregningsresultatEntitet nyttForeslåttResultat = beregningsresultatRepository.hentBeregningsresultat(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + ref.getBehandlingId()));
        BeregningsresultatEntitet forrigeForeslåttResultat = forrigeRes.getBgBeregningsresultatFP();

        // Hvis det foreslåtte resultatet er likt kan vi kopiere det utbetalte resultatet
        return SammenlignBeregningsresultat.erLike(nyttForeslåttResultat, forrigeForeslåttResultat);
    }

    public void kopierOgLagreUtbetBeregningsresultat(BehandlingReferanse ref) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        BehandlingBeregningsresultatEntitet forrigeRes = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid)).orElseThrow();
        BeregningsresultatEntitet forrigeUtbetResultat = forrigeRes.getUtbetBeregningsresultatFP();
        Boolean skulleHindreTilbaketrekkIForrigeBehandling = forrigeRes.skalHindreTilbaketrekk().orElseThrow();

        // Lagre original beslutning
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, skulleHindreTilbaketrekkIForrigeBehandling);

        if (skulleHindreTilbaketrekkIForrigeBehandling) {

            // Kopier utbet til ny entitet
            BeregningsresultatEntitet nyttUtbetResultat = lagKopi(forrigeUtbetResultat);

            boolean kopiErLikOriginal = SammenlignBeregningsresultat.erLike(nyttUtbetResultat, forrigeUtbetResultat);
            if (!kopiErLikOriginal) {
                throw new IllegalStateException("Gammelt og nytt utbetalt beregningsresultat er ikke likt for behandling " + ref.getBehandlingId());
            }

            // Beregn feriepenger
            var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
            feriepengerTjeneste.beregnFeriepenger(behandling, nyttUtbetResultat);

            // Lagre utbet entitet
            LOG.info("Lagrer kopiert utbetalt resultat på behandling med id " + ref.getBehandlingId() +
                " kopiert fra behandling med id " + ref.getOriginalBehandlingId().get());
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, nyttUtbetResultat);
        }
    }

    private BeregningsresultatEntitet lagKopi(BeregningsresultatEntitet forrigeUtbetResultat) {
        BeregningsresultatEntitet nyttUtbetResultat = BeregningsresultatEntitet.builder()
            .medRegelSporing(forrigeUtbetResultat.getRegelSporing())
            .medRegelInput(forrigeUtbetResultat.getRegelInput())
            .medEndringsdato(forrigeUtbetResultat.getEndringsdato().orElse(null))
            .build();

        forrigeUtbetResultat.getBeregningsresultatPerioder().forEach(utbPeriode -> {
            BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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
