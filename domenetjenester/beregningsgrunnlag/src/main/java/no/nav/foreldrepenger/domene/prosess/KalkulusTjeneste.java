package no.nav.foreldrepenger.domene.prosess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.PersonIdent;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.kalkulatorinput.KalkulusMapper;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.rest.KalkulusRestKlient;

@ApplicationScoped
public class KalkulusTjeneste {

    private KalkulusRestKlient kalkulusRestKlient;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BehandlingRepository behandlingRepository;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient kalkulusRestKlient,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                            BehandlingRepository behandlingRepository) {
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        var request = new BeregnListeRequest(behandlingReferanse.getSaksnummer().getVerdi(), map(behandlingReferanse),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            new StegType(behandlingStegType.getKode()), List.of(mapBeregnRequestForBehandling(behandlingReferanse)));
        var respons = kalkulusRestKlient.beregn(request);
        if (respons.getTilstand().size() != 1) {
            throw new IllegalStateException("Forventet å få akkurat en respons fra kalkulus, fikk " + respons.getTilstand().size());
        }
        var tilstandRespons = respons.getTilstand().get(0);
        var aksjonspunktliste = mapAvklaringsbehovResultatFraRespons(tilstandRespons);
        var resultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktliste);
        if (tilstandRespons.getVilkarOppfylt() != null) {
            // TODO trenger vi regelevaluering for beregning lagret i fp-sak? Brukes i generering av vedtak-xml
            resultat.setVilkårOppfylt(tilstandRespons.getVilkarOppfylt(), "", "");
        }
        return resultat;
    }

    private List<BeregningAvklaringsbehovResultat> mapAvklaringsbehovResultatFraRespons(TilstandResponse tilstandRespons) {
        var avklaringsbehovliste = new ArrayList<BeregningAvklaringsbehovResultat>();
        if (tilstandRespons.getAvklaringsbehovMedTilstandDto() != null) {
            tilstandRespons.getAvklaringsbehovMedTilstandDto().stream().
                map(ab -> BeregningAvklaringsbehovResultat.opprettFor(AvklaringsbehovDefinisjon.fraKode(ab.getBeregningAvklaringsbehovDefinisjon().getKode())))
                .forEach(avklaringsbehovliste::add);
        }
        return avklaringsbehovliste;
    }

    private BeregnForRequest mapBeregnRequestForBehandling(BehandlingReferanse behandlingReferanse) {
        var originalreferanser = behandlingReferanse.getOriginalBehandlingId()
            .map(behandlingRepository::hentBehandling)
            .map(Behandling::getUuid)
            .map(List::of)
            .orElse(Collections.emptyList());

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse, iayGrunnlag).orElseThrow(() -> new IllegalStateException("Ingen opptjeningsaktiviteter for beregning"));
        var kalkulatorInput = KalkulusMapper.mapKalkulatorInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter);
        return new BeregnForRequest(behandlingReferanse.getBehandlingUuid(), originalreferanser, kalkulatorInput, Collections.emptyList());
    }

    private PersonIdent map(BehandlingReferanse behandlingReferanse) {
        return new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
    }

}
