package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttakFraEndringsdato;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFellesImpl;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("SVP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFellesImpl {

    private SvangerskapspengerUttakResultatRepository uttakRepository;
    private EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                    HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    @FagsakYtelseTypeRef("SVP") EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder,
                                                    OpphørUttakTjeneste opphørUttakTjeneste,
                                                    UttakInputTjeneste uttakInputTjeneste,
                                                    @FagsakYtelseTypeRef("SVP") ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato,
                                                    @FagsakYtelseTypeRef("SVP") ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                    @FagsakYtelseTypeRef("SVP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
            beregningsgrunnlagTjeneste,
            medlemTjeneste,
            opphørUttakTjeneste,
            erEndringIUttakFraEndringsdato,
            erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
            skjæringstidspunktTjeneste
        );
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        return new UttakResultatHolderImpl(uttakRepository.hentHvisEksisterer(behandlingId));
    }

    @Override
    protected HarEtablertYtelse harEtablertYtelse() {
        return new HarEtablertYtelseImpl();
    }

    @Override
    protected LocalDate finnEndringsdato(BehandlingReferanse ref) {
        return endringsdatoRevurderingUtleder.utledEndringsdato(uttakInputTjeneste.lagInput(ref.getBehandlingId()));
    }
}
