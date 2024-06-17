package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.EnkelFpkalkulusRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.HentBeregningsgrunnlagGUIRequest;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

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
    public void beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {

    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse) {
        var kalkulusInput = kalkulusInputTjeneste.lagKalkulusInput(referanse);
        var hentGuiDtoRequest = new HentBeregningsgrunnlagGUIRequest(referanse.behandlingUuid(), kalkulusInput);
        return klient.hentGrunnlagGUI(hentGuiDtoRequest);
    }

    private EnkelFpkalkulusRequestDto lagEnkelKalkulusRequest(BehandlingReferanse referanse) {
        return new EnkelFpkalkulusRequestDto(referanse.behandlingUuid(), new Saksnummer(referanse.saksnummer().getVerdi()));
    }

}
