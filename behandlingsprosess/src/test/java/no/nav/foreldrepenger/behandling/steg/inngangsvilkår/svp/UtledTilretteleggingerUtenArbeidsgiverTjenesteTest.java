package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;

class UtledTilretteleggingerUtenArbeidsgiverTjenesteTest {

    @Test
    void skal_filtrer_bort_tilrettelegginger_med_arbeidsgiver() {

        // Arrange
        var tilrettelegging_1 = new SvpTilretteleggingEntitet.Builder().medArbeidType(ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .build();
        var tilrettelegging_2 = new SvpTilretteleggingEntitet.Builder().medArbeidType(ORDINÆRT_ARBEIDSFORHOLD).medArbeidsgiver(null).build();
        var tilrettelegging_3 = new SvpTilretteleggingEntitet.Builder().medArbeidType(ORDINÆRT_ARBEIDSFORHOLD).medArbeidsgiver(null).build();
        var tilrettelegging_4 = new SvpTilretteleggingEntitet.Builder().medArbeidType(ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .build();
        var tilrettelegginger = List.of(tilrettelegging_1, tilrettelegging_2, tilrettelegging_3, tilrettelegging_4);

        // Act
        var result = NyeTilretteleggingerTjeneste.utledUtenArbeidsgiver(tilrettelegginger);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getArbeidsgiver()).isEmpty();
        assertThat(result.get(1).getArbeidsgiver()).isEmpty();

    }

}
