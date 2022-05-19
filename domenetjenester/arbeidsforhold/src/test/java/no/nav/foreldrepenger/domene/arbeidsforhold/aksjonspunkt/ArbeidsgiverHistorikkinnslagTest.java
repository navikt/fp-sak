package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ExtendWith(JpaExtension.class)
public class ArbeidsgiverHistorikkinnslagTest {

    private static final String PRIVATPERSON_NAVN = "Mikke Mus";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String ORGNR = "999999999";
    private static final String ORG_NAVN = "Andeby Bank";
    private static final Virksomhet VIRKSOMHET = lagVirksomhet(ORGNR);
    private static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String SUFFIX = ARBEIDSFORHOLD_REF.getReferanse().substring(ARBEIDSFORHOLD_REF.getReferanse().length() - 4);

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    @BeforeEach
    void setup() {
        var personIdentTjeneste = mock(PersonIdentTjeneste.class);
        when(personIdentTjeneste.hentBrukerForAktør(any(AktørId.class))).thenReturn(Optional.of(lagPersoninfo()));

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(VIRKSOMHET);
        var arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(personIdentTjeneste, virksomhetTjeneste);
        arbeidsgiverHistorikkinnslag = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
    }

    private PersoninfoArbeidsgiver lagPersoninfo() {
        return new PersoninfoArbeidsgiver.Builder()
                .medAktørId(AKTØR_ID)
                .medPersonIdent(new PersonIdent("123"))
                .medFødselsdato(FØDSELSDATO)
                .medNavn(PRIVATPERSON_NAVN)
                .build();
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_privatperson_uten_arbref() {
        // Act
        var arbeidsgiverNavn = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.person(AKTØR_ID), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Mikke Mus (01.01.2000)");
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_privatperson_med_arbref() {
        // Act
        var arbeidsgiverNavn = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
                Arbeidsgiver.person(AKTØR_ID), ARBEIDSFORHOLD_REF, List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Mikke Mus (01.01.2000) ..." + SUFFIX);
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_uten_arbref() {
        // Act
        var arbeidsgiverNavn = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.virksomhet(ORGNR), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (" + ORGNR + ")");
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_med_arbref() {
        // Act
        var arbeidsgiverNavn = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
                Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_REF, List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (" + ORGNR + ") ..." + SUFFIX);
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_med_arbref_null() {
        // Act
        var arbeidsgiverNavn = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.virksomhet(ORGNR),
                InternArbeidsforholdRef.nullRef(), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (" + ORGNR + ")");
    }

    private static Virksomhet lagVirksomhet(String orgnr) {
        var b = new Virksomhet.Builder();
        b.medOrgnr(orgnr).medNavn(ORG_NAVN);
        return b.build();
    }

}
