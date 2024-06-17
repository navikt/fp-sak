package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnresultatgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAksjonspunkterDto;

@ApplicationScoped
public class TotrinnsaksjonspunktDtoTjeneste {
    private TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste;
    private TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste;
    private UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste;


    protected TotrinnsaksjonspunktDtoTjeneste() {
        // for CDI proxy
    }


    @Inject
    public TotrinnsaksjonspunktDtoTjeneste(TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste,
                                           UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste,
                                           TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste) {
        this.totrinnskontrollAktivitetDtoTjeneste = totrinnskontrollAktivitetDtoTjeneste;
        this.totrinnsBeregningDtoTjeneste = totrinnsBeregningDtoTjeneste;
        this.uttakPeriodeEndringDtoTjeneste = uttakPeriodeEndringDtoTjeneste;
    }

    public TotrinnskontrollAksjonspunkterDto lagTotrinnskontrollAksjonspunktDto(Totrinnsvurdering aksjonspunkt,
                                                                                Behandling behandling,
                                                                                Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        return new TotrinnskontrollAksjonspunkterDto.Builder().medAksjonspunktKode(aksjonspunkt.getAksjonspunktDefinisjon().getKode())
            .medOpptjeningAktiviteter(totrinnskontrollAktivitetDtoTjeneste.hentAktiviterEndretForOpptjening(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid)))
            .medBeregningDto(totrinnsBeregningDtoTjeneste.hentBeregningDto(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getBeregningsgrunnlagId)))
            .medBesluttersBegrunnelse(aksjonspunkt.getBegrunnelse())
            .medTotrinnskontrollGodkjent(aksjonspunkt.isGodkjent())
            .medVurderPaNyttArsaker(hentVurderPåNyttÅrsaker(aksjonspunkt))
            .medEndretUttakPerioder(uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(aksjonspunkt, behandling, totrinnresultatgrunnlag))
            .build();
    }

    private Set<VurderÅrsak> hentVurderPåNyttÅrsaker(Totrinnsvurdering aksjonspunkt) {
        return aksjonspunkt.getVurderPåNyttÅrsaker().stream().map(VurderÅrsakTotrinnsvurdering::getÅrsaksType).collect(Collectors.toSet());
    }
}
