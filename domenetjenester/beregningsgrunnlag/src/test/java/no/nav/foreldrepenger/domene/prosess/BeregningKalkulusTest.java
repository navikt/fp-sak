package no.nav.foreldrepenger.domene.prosess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningSteg;
import no.nav.folketrygdloven.kalkulus.response.v1.tilstander.TilgjengeligeTilstanderDto;

import org.jboss.weld.exceptions.IllegalStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.beregningsgrunnlag.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.behandlingslager.beregningsgrunnlag.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class BeregningKalkulusTest {
    @Mock
    private KalkulusKlient kalkulusKlient;
    @Mock
    private KalkulusInputTjeneste kalkulusInputTjeneste;
    @Mock
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    @Mock
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BeregningKalkulus beregningKalkulus;

    @BeforeEach
    void setup() {
        beregningKalkulus = new BeregningKalkulus(kalkulusKlient, kalkulusInputTjeneste, koblingRepository, besteberegningFødendeKvinneTjeneste, skjæringstidspunktTjeneste);
    }

    @Test
    void skal_kjøre_opprette_kobling_første_steg() {
        // Arrange
        var behandlingReferanse = lagRef();
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.empty());
        when(koblingRepository.opprettKobling(behandlingReferanse.behandlingId(), behandlingReferanse.behandlingUuid())).thenReturn(new BeregningsgrunnlagKobling(behandlingReferanse
            .behandlingId(), behandlingReferanse.behandlingUuid()));
        when(kalkulusKlient.beregn(any())).thenReturn(new KalkulusRespons(List.of(), new KalkulusRespons.VilkårRespons(true, "", "", "")));
        // Act
        beregningKalkulus.beregn(behandlingReferanse, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);

        // Assert
        verify(koblingRepository, times(1)).opprettKobling(behandlingReferanse.behandlingId(), behandlingReferanse.behandlingUuid());
        verify(kalkulusKlient, times(1)).beregn(any());
    }

    @Test
    void skal_kaste_feil_når_det_mangler_kobling_i_senere_steg() {
        // Arrange
        var behandlingReferanse = lagRef();
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> beregningKalkulus.beregn(behandlingReferanse, BehandlingStegType.KONTROLLER_FAKTA_BEREGNING));
    }

    @Test
    void skal_fortsette_beregning_når_kobling_finnes() {
        // Arrange
        var behandlingReferanse = lagRef();
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.of(new BeregningsgrunnlagKobling(behandlingReferanse
            .behandlingId(), behandlingReferanse.behandlingUuid())));
        when(kalkulusKlient.beregn(any())).thenReturn(new KalkulusRespons(List.of(), new KalkulusRespons.VilkårRespons(true, "", "", "")));

        // Act
        beregningKalkulus.beregn(behandlingReferanse, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        // Assert
        verify(koblingRepository, times(0)).opprettKobling(behandlingReferanse.behandlingId(), behandlingReferanse.behandlingUuid());
        verify(kalkulusKlient, times(1)).beregn(any());
    }

    @Test
    void skal_kalle_kopier_endepunkt_når_koblinger_matcher_saksnummer() {
        // Arrange
        var behandlingReferanse = lagRef(1L, "123");
        var behandlingReferanse2 = lagRef(2L, "123");
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.of(new BeregningsgrunnlagKobling(behandlingReferanse
            .behandlingId(), behandlingReferanse.behandlingUuid())));
        when(koblingRepository.hentKobling(behandlingReferanse2.behandlingId())).thenReturn(Optional.of(new BeregningsgrunnlagKobling(behandlingReferanse2
            .behandlingId(), behandlingReferanse2.behandlingUuid())));

        // Act
        beregningKalkulus.kopier(behandlingReferanse, behandlingReferanse2, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG);

        // Assert
        verify(kalkulusKlient, times(1)).kopierGrunnlag(any());
    }

    @Test
    void skal_feile_når_koblinger_tilhører_ulike_saker() {
        // Arrange
        var behandlingReferanse = lagRef(1L, "123");
        var behandlingReferanse2 = lagRef(2L, "321");

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> beregningKalkulus.kopier(behandlingReferanse, behandlingReferanse2, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG));
    }

    @Test
    void skal_returnere_hvilke_steg_som_er_tilgjengelige() {
        // Arrange
        var behandlingReferanse = lagRef(1L, "123");
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.of(new BeregningsgrunnlagKobling(behandlingReferanse
            .behandlingId(), behandlingReferanse.behandlingUuid())));
        when(kalkulusKlient.hentTilgjengeligeTilstander(any())).thenReturn(new TilgjengeligeTilstanderDto(new TilgjengeligeTilstanderDto.TilgjengeligeTilstandDto(behandlingReferanse.behandlingUuid(), List.of(
            BeregningSteg.FASTSETT_STP_BER, BeregningSteg.KOFAKBER, BeregningSteg.FORS_BERGRUNN, BeregningSteg.FORS_BERGRUNN_2, BeregningSteg.VURDER_VILKAR_BERGRUNN)), null));

        // Act
        var kanStarteISteg = beregningKalkulus.kanStartesISteg(behandlingReferanse, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        assertThat(kanStarteISteg).isTrue();
    }


    private static BehandlingReferanse lagRef() {
        return lagRef(1L, "1234");
    }

    private static BehandlingReferanse lagRef(Long behandlingId, String saksnumer) {
        return new BehandlingReferanse(new Saksnummer(saksnumer),
            1234L,
            FagsakYtelseType.SVANGERSKAPSPENGER,
            behandlingId,
            UUID.randomUUID(),
            BehandlingStatus.UTREDES,
            BehandlingType.FØRSTEGANGSSØKNAD,
            null,
            new AktørId("9999999999999"),
            RelasjonsRolleType.MORA);
    }

}
