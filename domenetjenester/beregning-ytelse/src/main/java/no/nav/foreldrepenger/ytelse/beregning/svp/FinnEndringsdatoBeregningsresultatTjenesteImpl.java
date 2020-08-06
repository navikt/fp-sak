package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.time.LocalDate;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoFeil;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoMellomPeriodeLister;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class FinnEndringsdatoBeregningsresultatTjenesteImpl implements FinnEndringsdatoBeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister;

    FinnEndringsdatoBeregningsresultatTjenesteImpl() {
        // NOSONAR
    }

    @Inject
    public FinnEndringsdatoBeregningsresultatTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                                             FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
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
        if (originalePerioder.isEmpty()) {
            Long id = originalBeregningsresultat.getId();
            throw FinnEndringsdatoFeil.FACTORY.manglendeBeregningsresultatPeriode(id).toException();
        }
        List<BeregningsresultatPeriode> revurderingPerioder = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        if (revurderingPerioder.isEmpty()) {
            //Dersom det ikke er noen ny perioder i revurdering, så bruk første tilretteleggingsbehovsdato
            var grunnlagOpt = svangerskapspengerRepository.hentGrunnlag(revurdering.getId());
            if (grunnlagOpt.isPresent()) {
                return new TilretteleggingFilter(grunnlagOpt.get()).getFørsteTilretteleggingsbehovdatoFiltrert();
            }
        }
        return finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingPerioder, originalePerioder);
    }

}
