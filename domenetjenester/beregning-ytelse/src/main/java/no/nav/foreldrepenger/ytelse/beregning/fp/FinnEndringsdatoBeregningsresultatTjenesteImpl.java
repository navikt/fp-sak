package no.nav.foreldrepenger.ytelse.beregning.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.*;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatEndringModell;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FinnEndringsdatoBeregningsresultatTjenesteImpl implements FinnEndringsdatoBeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private FinnEndringsdatoForBeregningsresultat finnEndringsdatoFraBeregningsresultat;

    FinnEndringsdatoBeregningsresultatTjenesteImpl() {
        // NOSONAR
    }

    @Inject
    public FinnEndringsdatoBeregningsresultatTjenesteImpl(BeregningsresultatRepository beregningsresultatRepository,
                                                          FinnEndringsdatoForBeregningsresultat finnEndringsdatoFraBeregningsresultat) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.finnEndringsdatoFraBeregningsresultat = finnEndringsdatoFraBeregningsresultat;
    }

    @Override
    public Optional<LocalDate> finnEndringsdato(Behandling behandling, BeregningsresultatEntitet revurderingBeregningsresultat) {
        if (behandling.getType().equals(BehandlingType.REVURDERING)) {
            return finnEndringsdatoForRevurdering(behandling, revurderingBeregningsresultat);
        } else {
            throw FinnEndringsdatoFeil.FACTORY.behandlingErIkkeEnRevurdering(behandling.getId()).toException();
        }
    }

    private Optional<LocalDate> finnEndringsdatoForRevurdering(Behandling revurdering, BeregningsresultatEntitet revurderingBeregningsresultat) {
        Long originalBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> FinnEndringsdatoFeil.FACTORY.manglendeOriginalBehandling(revurdering.getId()).toException());
        Optional<BeregningsresultatEntitet> originalBeregningsresultatFPOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(originalBehandlingId);
        if (originalBeregningsresultatFPOpt.isEmpty()) {
            return Optional.empty();
        }
        BeregningsresultatEntitet originalBeregningsresultat = originalBeregningsresultatFPOpt.get();
        List<BeregningsresultatPeriode> originalePerioder = originalBeregningsresultat.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> revurderingPerioder = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        if (originalePerioder.isEmpty()) {
            if (revurderingPerioder.isEmpty()) {
                // Dette skal ikkje vere mulig for foreldrepenger?
                Long id = revurderingBeregningsresultat.getId();
                throw FinnEndringsdatoFeil.FACTORY.manglendeBeregningsresultatPeriode(id).toException();
            }
            return revurderingPerioder.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).min(Comparator.naturalOrder());
        }
        if (revurderingPerioder.isEmpty()) {
            Long id = revurderingBeregningsresultat.getId();
            throw FinnEndringsdatoFeil.FACTORY.manglendeBeregningsresultatPeriode(id).toException();
        }
        BeregningsresultatEndringModell originalRegelmodell = new MapBeregningsresultatTilEndringsmodell(originalBeregningsresultat).map();
        BeregningsresultatEndringModell revurderingRegelmodell = new MapBeregningsresultatTilEndringsmodell(revurderingBeregningsresultat).map();
        return finnEndringsdatoFraBeregningsresultat.utledEndringsdato(originalRegelmodell, revurderingRegelmodell);
    }

}
