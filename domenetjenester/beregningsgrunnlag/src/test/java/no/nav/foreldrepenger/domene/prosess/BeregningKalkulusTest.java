package no.nav.foreldrepenger.domene.prosess;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class BeregningKalkulusTest {
    @Mock
    private KalkulusKlient kalkulusKlient;
    @Mock
    private KalkulusInputTjeneste kalkulusInputTjeneste;
    @Mock
    private BeregningsgrunnlagKoblingRepository koblingRepository;

    private BeregningKalkulus beregningKalkulus;

    @BeforeEach
    void setup() {
        beregningKalkulus = new BeregningKalkulus(kalkulusKlient, kalkulusInputTjeneste, koblingRepository);
    }

    @Test
    void skal_kjøre_opprette_kobling_første_steg() {
        // Arrange
        var behandlingReferanse = lagRef();
        when(koblingRepository.hentKobling(behandlingReferanse.behandlingId())).thenReturn(Optional.empty());
        when(koblingRepository.opprettKobling(behandlingReferanse)).thenReturn(new BeregningsgrunnlagKobling(behandlingReferanse
            .behandlingId(), behandlingReferanse.behandlingUuid()));
        when(kalkulusKlient.beregn(any())).thenReturn(new KalkulusRespons(List.of(), true));
        // Act
        beregningKalkulus.beregn(behandlingReferanse, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);

        // Assert
        verify(koblingRepository, times(1)).opprettKobling(behandlingReferanse);
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
        when(kalkulusKlient.beregn(any())).thenReturn(new KalkulusRespons(List.of(), true));

        // Act
        beregningKalkulus.beregn(behandlingReferanse, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        // Assert
        verify(koblingRepository, times(0)).opprettKobling(behandlingReferanse);
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
        beregningKalkulus.kopier(behandlingReferanse, behandlingReferanse2, BeregningsgrunnlagTilstand.FASTSATT);

        // Assert
        verify(kalkulusKlient, times(1)).kopierGrunnlag(any());
    }

    @Test
    void skal_feile_når_koblinger_tilhører_ulike_saker() {
        // Arrange
        var behandlingReferanse = lagRef(1L, "123");
        var behandlingReferanse2 = lagRef(2L, "321");

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> beregningKalkulus.kopier(behandlingReferanse, behandlingReferanse2, BeregningsgrunnlagTilstand.FASTSATT));
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
