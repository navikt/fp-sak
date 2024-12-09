package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class NyeTilretteleggingerTjenesteTest {

    private final SvangerskapspengerRepository svangerskapspengerRepository = Mockito.mock(SvangerskapspengerRepository.class);
    private final UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste = Mockito
            .mock(UtledTilretteleggingerMedArbeidsgiverTjeneste.class);
    private final NyeTilretteleggingerTjeneste utledNyeTilretteleggingerTjeneste = new NyeTilretteleggingerTjeneste(
            svangerskapspengerRepository, utledTilretteleggingerMedArbeidsgiverTjeneste);

    @Test
    void skal_utlede_tilrettelegginger_med_og_uten_arbeidsgiver() {

        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().build();

        var tilretteleggingArbeidUtenInternId = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123")).build();
        var tilretteleggingArbeidMedInternId = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123")).medInternArbeidsforholdRef(InternArbeidsforholdRef.nyRef()).build();
        var tilretteleggingFrilans = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.FRILANSER).build();

        var tilretteleggingEntiteter = List.of(tilretteleggingArbeidUtenInternId, tilretteleggingFrilans);
        when(utledTilretteleggingerMedArbeidsgiverTjeneste.utled(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyList()))
                .thenReturn(List.of(tilretteleggingArbeidMedInternId));

        var grunnlagEntitet = new SvpGrunnlagEntitet.Builder()
                .medOpprinneligeTilrettelegginger(List.of(tilretteleggingArbeidUtenInternId, tilretteleggingFrilans))
                .medBehandlingId(behandling.getId())
                .build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.of(grunnlagEntitet));

        // Act
        var result = utledNyeTilretteleggingerTjeneste.utledJusterte(behandling, skjæringstidspunkt, grunnlagEntitet.getGjeldendeVersjon().getTilretteleggingListe() );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(NyeTilretteleggingerTjeneste.likeTilrettelegginger(result, tilretteleggingEntiteter)).isFalse();

    }

    @Test
    void skal_ikke_justere_tilrettelegginger_med_og_uten_arbeidsgiver() {

        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().build();

        var tilretteleggingArbeidUtenInternId = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123")).build();
        var tilretteleggingFrilans = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.FRILANSER).build();

        var tilretteleggingEntiteter = List.of(tilretteleggingArbeidUtenInternId, tilretteleggingFrilans);
        when(utledTilretteleggingerMedArbeidsgiverTjeneste.utled(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyList()))
            .thenReturn(List.of(tilretteleggingArbeidUtenInternId));

        var grunnlagEntitet = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(tilretteleggingArbeidUtenInternId, tilretteleggingFrilans))
            .medBehandlingId(behandling.getId())
            .build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.of(grunnlagEntitet));

        // Act
        var result = utledNyeTilretteleggingerTjeneste.utledJusterte(behandling, skjæringstidspunkt, grunnlagEntitet.getGjeldendeVersjon().getTilretteleggingListe());

        // Assert
        assertThat(result).hasSize(2);

        assertThat(NyeTilretteleggingerTjeneste.likeTilrettelegginger(result, tilretteleggingEntiteter)).isTrue();
    }

    @Test
    void skal_kaste_exception_når_ingen_grunnlag_blir_funnet() {
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var skjæringstidspunkt = Skjæringstidspunkt.builder().build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> utledNyeTilretteleggingerTjeneste.utledNyeTilretteleggingerLagreJustert(behandling, skjæringstidspunkt));
    }
}
