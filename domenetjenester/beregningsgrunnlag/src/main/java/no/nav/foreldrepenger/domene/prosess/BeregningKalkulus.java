package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.BeregnRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.EnkelFpkalkulusRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.FpkalkulusYtelser;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.HentBeregningsgrunnlagGUIRequest;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningSteg;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {

    private KalkulusKlient klient;
    private KalkulusInputTjeneste kalkulusInputTjeneste;

    BeregningKalkulus() {
        // CDI
    }

    @Inject
    public BeregningKalkulus(KalkulusKlient klient, KalkulusInputTjeneste kalkulusInputTjeneste) {
        this.klient = klient;
        this.kalkulusInputTjeneste = kalkulusInputTjeneste;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        var request = lagEnkelKalkulusRequest(referanse);
        return klient.hentGrunnlag(request).map(KalkulusTilFpsakMapper::map);
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {
        // TODO Her må vi sjekke i fpsak om vi har lagret en referanse lokalt
        var beregningSteg = mapTilBeregningStegType(stegType);
        var request = lagBeregningRequest(behandlingReferanse, beregningSteg);
        var respons = klient.beregn(request);
        var prosessResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(respons.aksjonspunkter());
        // TODO Finn ut hvordan vi løser sporing av vilkåret
        prosessResultat.setVilkårOppfylt(respons.erVilkårOppfylt(), null, null, null);
        return prosessResultat;
    }

    private BeregnRequestDto lagBeregningRequest(BehandlingReferanse behandlingReferanse, BeregningSteg beregningSteg) {
        var saksnummer = new Saksnummer(behandlingReferanse.saksnummer().getVerdi());
        var personIdent = new AktørIdPersonident(behandlingReferanse.aktørId().getId());
        var ytelse = mapSkalBeregneYtelsetype(behandlingReferanse.fagsakYtelseType());
        var input = kalkulusInputTjeneste.lagKalkulusInput(behandlingReferanse);
        return new BeregnRequestDto(saksnummer, behandlingReferanse.behandlingUuid(), personIdent, ytelse, beregningSteg, input, null);
    }

    private FpkalkulusYtelser mapSkalBeregneYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> FpkalkulusYtelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FpkalkulusYtelser.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Ukjent ytelse som skal beregnes " + fagsakYtelseType);
        };
    }

    private BeregningSteg mapTilBeregningStegType(BehandlingStegType stegType) {
        return switch (stegType) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING -> BeregningSteg.FASTSETT_STP_BER;
            case KONTROLLER_FAKTA_BEREGNING -> BeregningSteg.KOFAKBER;
            case FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN;
            case FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN_2;
            case FORESLÅ_BESTEBEREGNING -> BeregningSteg.FORS_BESTEBEREGNING;
            case VURDER_VILKAR_BERGRUNN -> BeregningSteg.VURDER_VILKAR_BERGRUNN;
            case VURDER_REF_BERGRUNN -> BeregningSteg.VURDER_REF_BERGRUNN;
            case FORDEL_BEREGNINGSGRUNNLAG -> BeregningSteg.FORDEL_BERGRUNN;
            case FASTSETT_BEREGNINGSGRUNNLAG -> BeregningSteg.FAST_BERGRUNN;
            default -> throw new IllegalStateException("Ukjent beregningssteg " + stegType);
        };
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse) {
        var kalkulusInput = kalkulusInputTjeneste.lagKalkulusInput(referanse);
        var hentGuiDtoRequest = new HentBeregningsgrunnlagGUIRequest(referanse.behandlingUuid(), kalkulusInput);
        return klient.hentGrunnlagGUI(hentGuiDtoRequest);
    }

    private EnkelFpkalkulusRequestDto lagEnkelKalkulusRequest(BehandlingReferanse referanse) {
        return new EnkelFpkalkulusRequestDto(referanse.behandlingUuid(),
            new Saksnummer(referanse.saksnummer().getVerdi()));
    }

}
