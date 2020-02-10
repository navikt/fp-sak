package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollVurderÅrsak;

@ApplicationScoped
public class TotrinnsaksjonspunktDtoTjeneste {
    private TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste;
    private TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste;
    private UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste;
    private TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste;


    protected TotrinnsaksjonspunktDtoTjeneste() {
        // for CDI proxy
    }


    @Inject
    public TotrinnsaksjonspunktDtoTjeneste(TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste,
                                           UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste,
                                           TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste,
                                           TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste) {
        this.totrinnskontrollAktivitetDtoTjeneste = totrinnskontrollAktivitetDtoTjeneste;
        this.totrinnsBeregningDtoTjeneste = totrinnsBeregningDtoTjeneste;
        this.uttakPeriodeEndringDtoTjeneste = uttakPeriodeEndringDtoTjeneste;
        this.totrinnArbeidsforholdDtoTjeneste = totrinnArbeidsforholdDtoTjeneste;
    }

    public TotrinnskontrollAksjonspunkterDto lagTotrinnskontrollAksjonspunktDto(Totrinnsvurdering aksjonspunkt,
                                                                                 Behandling behandling,
                                                                                 Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        return new TotrinnskontrollAksjonspunkterDto.Builder()
            .medAksjonspunktKode(aksjonspunkt.getAksjonspunktDefinisjon().getKode())
            .medOpptjeningAktiviteter(totrinnskontrollAktivitetDtoTjeneste.hentAktiviterEndretForOpptjening(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid)))
            .medBeregningDto(totrinnsBeregningDtoTjeneste.hentBeregningDto(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getBeregningsgrunnlagId)))
            .medBesluttersBegrunnelse(aksjonspunkt.getBegrunnelse())
            .medArbeidsforhold(totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, aksjonspunkt,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid)))
            .medTotrinnskontrollGodkjent(aksjonspunkt.isGodkjent())
            .medVurderPaNyttArsaker(hentVurderPåNyttÅrsaker(aksjonspunkt))
            .medEndretUttakPerioder(uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(aksjonspunkt, behandling, totrinnresultatgrunnlag))
            .build();
    }

    private Set<TotrinnskontrollVurderÅrsak> hentVurderPåNyttÅrsaker(Totrinnsvurdering aksjonspunkt) {
        return aksjonspunkt.getVurderPåNyttÅrsaker().stream()
            .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType)
            .map(arsakType -> new TotrinnskontrollVurderÅrsak(arsakType.getKode(), arsakType.getNavn()))
            .collect(Collectors.toSet());
    }
}
