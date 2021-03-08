package no.nav.foreldrepenger.ytelse.beregning.fp;

import static no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoFeil.behandlingErIkkeEnRevurdering;
import static no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoFeil.manglendeBeregningsresultatPeriode;
import static no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoFeil.manglendeOriginalBehandling;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoMellomPeriodeLister;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FinnEndringsdatoBeregningsresultatTjenesteImpl implements FinnEndringsdatoBeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister;

    FinnEndringsdatoBeregningsresultatTjenesteImpl() {
        // NOSONAR
    }

    @Inject
    public FinnEndringsdatoBeregningsresultatTjenesteImpl(BeregningsresultatRepository beregningsresultatRepository,
                                                          FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.finnEndringsdatoMellomPeriodeLister = finnEndringsdatoMellomPeriodeLister;
    }

    @Override
    public Optional<LocalDate> finnEndringsdato(Behandling behandling,
                                                BeregningsresultatEntitet revurderingBeregningsresultat) {
        if (behandling.getType().equals(BehandlingType.REVURDERING)) {
            return finnEndringsdatoForRevurdering(behandling, revurderingBeregningsresultat);
        }
        throw behandlingErIkkeEnRevurdering(behandling.getId());
    }

    private Optional<LocalDate> finnEndringsdatoForRevurdering(Behandling revurdering,
                                                               BeregningsresultatEntitet revurderingBeregningsresultat) {
        var originalBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> manglendeOriginalBehandling(revurdering.getId()));
        var originalBeregningsresultatFPOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(
            originalBehandlingId);
        if (originalBeregningsresultatFPOpt.isEmpty()) {
            return Optional.empty();
        }
        var originalBeregningsresultat = originalBeregningsresultatFPOpt.get();
        var originalePerioder = originalBeregningsresultat.getBeregningsresultatPerioder();
        var revurderingPerioder = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        if (originalePerioder.isEmpty()) {
            if (revurderingPerioder.isEmpty()) {
                // Dette skal ikkje vere mulig for foreldrepenger?
                var id = revurderingBeregningsresultat.getId();
                throw manglendeBeregningsresultatPeriode(id);
            }
            return revurderingPerioder.stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
        }
        if (revurderingPerioder.isEmpty()) {
            var id = revurderingBeregningsresultat.getId();
            throw manglendeBeregningsresultatPeriode(id);
        }
        return finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingPerioder, originalePerioder);
    }

}
