package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIBeregning;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.BehandlingsresultatBeregningTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class BehandlingsresultatBeregningTjenesteSVP implements BehandlingsresultatBeregningTjeneste {
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    public BehandlingsresultatBeregningTjenesteSVP() {
        // CDI
    }

    @Inject
    public BehandlingsresultatBeregningTjenesteSVP(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.svangerskapspengerUttakResultatRepository = svangerskapspengerUttakResultatRepository;
    }

    @Override
    public boolean erUgunst(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> revurderingBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.getBehandlingId());
        Optional<BeregningsgrunnlagEntitet> forrigeBG = ref.getOriginalBehandlingId().flatMap(beregningsgrunnlagTjeneste::hentBeregningsgrunnlagEntitetForBehandling);
        Optional<SvangerskapspengerUttakResultatEntitet> svpUttak = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        Optional<LocalDate> sisteDagMedSVP = svpUttak.flatMap(SvangerskapspengerUttakResultatEntitet::finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad);
        return ErEndringIBeregning.vurderUgunst(revurderingBG, forrigeBG, sisteDagMedSVP);
    }

    @Override
    public boolean erEndring(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> revurderingBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.getBehandlingId());
        Optional<BeregningsgrunnlagEntitet> forrigeBG = ref.getOriginalBehandlingId().flatMap(beregningsgrunnlagTjeneste::hentBeregningsgrunnlagEntitetForBehandling);
        Optional<SvangerskapspengerUttakResultatEntitet> svpUttak = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        Optional<LocalDate> sisteDagMedSVP = svpUttak.flatMap(SvangerskapspengerUttakResultatEntitet::finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad);
        return ErEndringIBeregning.vurder(revurderingBG, forrigeBG, sisteDagMedSVP);
    }

}
