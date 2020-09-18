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
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoFeil;
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
        return finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingPerioder, originalePerioder);
    }

}
