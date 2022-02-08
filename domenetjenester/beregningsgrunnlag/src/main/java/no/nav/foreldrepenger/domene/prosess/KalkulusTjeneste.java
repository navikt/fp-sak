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
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.kalkulatorinput.KalkulusInputMapper;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.rest.KalkulusRestKlient;

@ApplicationScoped
public class KalkulusTjeneste {

    private KalkulusRestKlient kalkulusRestKlient;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BehandlingRepository behandlingRepository;
    private KalkulusInputMapper kalkulusInputMapper;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient kalkulusRestKlient,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                            BehandlingRepository behandlingRepository, KalkulusInputMapper kalkulusInputMapper) {
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.kalkulusInputMapper = kalkulusInputMapper;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        var request = lagBeregnRequest(behandlingReferanse, behandlingStegType);
        var respons = kalkulusRestKlient.beregn(request);
        return mapTilBeregningResultat(respons);
    }

    public BeregningsgrunnlagGrunnlagDto hentGrunnlag(BehandlingReferanse behandlingReferanse) {
        HentBeregningsgrunnlagListeRequest hentRequest = lagHentRequest(behandlingReferanse);
        var grunnlagListe = kalkulusRestKlient.hentBeregningsgrunnlagGrunnlag(hentRequest);
        if (grunnlagListe.size() != 1) {
            throw new IllegalStateException("Forventet å finne nøyaktig ett grunnlag. Fant " + grunnlagListe.size());
        }
        return grunnlagListe.get(0);
    }

    public void deaktiver(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        BeregningsgrunnlagListeRequest deaktiverRequest = lagDeaktiverRequest(behandling);
        kalkulusRestKlient.deaktiverBeregningsgrunnlag(deaktiverRequest);
    }

    private BeregnListeRequest lagBeregnRequest(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        return new BeregnListeRequest(behandlingReferanse.getSaksnummer().getVerdi(), map(behandlingReferanse),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            new StegType(behandlingStegType.getKode()), List.of(mapBeregnRequestForBehandling(behandlingReferanse)));
    }

    private BeregningsgrunnlagListeRequest lagDeaktiverRequest(Behandling behandling) {
        return new BeregningsgrunnlagListeRequest(behandling.getFagsak().getSaksnummer().getVerdi(),
            List.of(new BeregningsgrunnlagRequest(behandling.getUuid())));
    }

    private HentBeregningsgrunnlagListeRequest lagHentRequest(BehandlingReferanse behandlingReferanse) {
        return new HentBeregningsgrunnlagListeRequest(List.of(new HentBeregningsgrunnlagRequest(behandlingReferanse.getBehandlingUuid(), behandlingReferanse.getSaksnummer().getVerdi(), YtelseTyperKalkulusStøtterKontrakt.fraKode(
            behandlingReferanse.getFagsakYtelseType().getKode()))),
            behandlingReferanse.getBehandlingUuid(), behandlingReferanse.getSaksnummer().getVerdi(), false);
    }

    private BeregningsgrunnlagVilkårOgAkjonspunktResultat mapTilBeregningResultat(TilstandListeResponse respons) {
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
        var kalkulatorInput = kalkulusInputMapper.mapKalkulatorInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter);
        return new BeregnForRequest(behandlingReferanse.getBehandlingUuid(), originalreferanser, kalkulatorInput, Collections.emptyList());
    }

    private PersonIdent map(BehandlingReferanse behandlingReferanse) {
        return new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
    }

}
