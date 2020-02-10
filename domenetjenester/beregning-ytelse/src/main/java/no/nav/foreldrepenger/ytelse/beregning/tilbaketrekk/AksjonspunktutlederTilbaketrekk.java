package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AksjonspunktutlederTilbaketrekk implements AksjonspunktUtleder {

    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    AksjonspunktutlederTilbaketrekk() {
    }

    @Inject
    public AksjonspunktutlederTilbaketrekk(BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        if (skalVurdereTilbaketrekk(param.getRef(), finnYrkesaktiviteter(param))) {
            AksjonspunktResultat aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_TILBAKETREKK);
            aksjonspunktResultater.add(aksjonspunktResultat);
        }

        return aksjonspunktResultater;
    }

    private boolean skalVurdereTilbaketrekk(BehandlingReferanse ref, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje =  beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(ref);
        return VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, yrkesaktiviteter, ref.getUtledetSkjæringstidspunkt());
    }


    private Collection<Yrkesaktivitet> finnYrkesaktiviteter(AksjonspunktUtlederInput param) {
        Optional<AktørArbeid> aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId()).getAktørArbeidFraRegister(param.getAktørId());
        return aktørArbeidFraRegister
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList());
    }
}
