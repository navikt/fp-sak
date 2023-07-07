package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Tjeneste som setter opp tidslinjen som brukes til å sammenligne beregningsresultatet mellom originalbehandlingen og revurderingen.
 * Brukes for å sjekke om søker har mistet penger til arbeidsgiver i løpet av beregningsresultatet mellom behandlingene.
 */
@ApplicationScoped
public class BeregningsresultatTidslinjetjeneste {
    private BeregningsresultatRepository beregningsresultatRepository;

    protected BeregningsresultatTidslinjetjeneste() {
        // CDI
    }

    @Inject
    public BeregningsresultatTidslinjetjeneste(BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    /**
     *
     * @param ref revurderingens behandlingreferanse
     * @return tidslinje over revurderingen og originalbehandlingens beregningsresultat
     */
    public LocalDateTimeline<BRAndelSammenligning> lagTidslinjeForRevurdering(BehandlingReferanse ref) {
        verifiserAtBehandlingErRevurdering(ref);
        var behandlingId = ref.behandlingId();

        // Nytt resultat, her aksepterer vi ikke tomt resultat siden vi har kommet til steget der vi vurderer beregningsresultatet.
        var revurderingBeregningsresultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        // Gammelt resultat, kan være tomt (f.eks ved avslått)
        var originaltBeregningsresultat = ref.getOriginalBehandlingId().flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat);

        var resultatperiodeRevurdering = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> resultatperiodeOriginal = originaltBeregningsresultat.isPresent() ? originaltBeregningsresultat.get().getBeregningsresultatPerioder() : Collections.emptyList();

        return mapTidslinje(resultatperiodeOriginal, resultatperiodeRevurdering);
    }

    private void verifiserAtBehandlingErRevurdering(BehandlingReferanse ref) {
        if (!ref.erRevurdering()) {
            throw new IllegalStateException("Kan ikke opprette beregningsresultattidslinje for noe annet enn revurderinger");
        }
    }

    private LocalDateTimeline<BRAndelSammenligning> mapTidslinje(List<BeregningsresultatPeriode> originaltBeregningsresultat,
                                                                 List<BeregningsresultatPeriode> revurderingBeregningsresultat) {
        return MapBRAndelSammenligningTidslinje.opprettTidslinje(
            originaltBeregningsresultat,
            revurderingBeregningsresultat);
    }

}
