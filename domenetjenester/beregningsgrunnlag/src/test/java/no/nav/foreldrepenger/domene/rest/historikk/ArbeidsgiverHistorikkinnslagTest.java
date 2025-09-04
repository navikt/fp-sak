package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class ArbeidsgiverHistorikkinnslagTest {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);

    @Test
    void skal_lage_navn_for_virksomhet() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref = InternArbeidsforholdRef.nyRef();
        when(arbeidsgiverTjeneste.hent(ag)).thenReturn(new ArbeidsgiverOpplysninger(ag.getIdentifikator(), "AG1"));
        var tjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);

        // Act
        var tekst = tjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, Optional.of(ag), Optional.of(ref),
            Collections.emptyList());

        // Assert
        assertThat(tekst).isEqualTo("AG1 (" + ag.getIdentifikator() + ") ..." + ref.getReferanse()
            .substring(ref.getReferanse().length() - 4));
    }

}
