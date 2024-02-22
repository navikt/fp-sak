package no.nav.foreldrepenger.domene.uttak.fakta.uttak;


import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.LoggInfoOmArbeidsforholdAktivitetskrav.loggInfoOmArbeidsforhold;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class LoggInfoOmArbeidsforholdAktivitetskravTest {


    @Test
    void en_periode_med_ett_arbeidsforhold_aktivitetskrav_ok() {
        var fraDato = LocalDate.of(2020, 1, 1);
        var tilDato = LocalDate.of(2021, 1, 1);
        var harAnnenForelderRett = true;

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(fraDato, tilDato)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
            .build();
        var aktuellePerioder = List.of(periode);

        var arbeidsforholdUtenPerminsjon = new ArbeidsforholdMedPermisjon(
            Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            null,
            List.of(arbeidsavtale(fraDato, tilDato, 100)),
            List.of()
        );
        var arbeidsforholdMedPermisjon = new ArbeidsforholdMedPermisjon(
            Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            null,
            List.of(arbeidsavtale(fraDato, tilDato, 50)),
            List.of(permisjon(fraDato, tilDato, 100))
        );
        var arbeidsforholdInfo = List.of(arbeidsforholdUtenPerminsjon, arbeidsforholdMedPermisjon);

        assertThatCode(() -> loggInfoOmArbeidsforhold(fraDato, tilDato, harAnnenForelderRett, aktuellePerioder, arbeidsforholdInfo))
            .doesNotThrowAnyException();
    }

    private static ArbeidsforholdTjeneste.AktivitetAvtale arbeidsavtale(LocalDate fraDato, LocalDate tilDato, int stillingsprosent) {
        return new ArbeidsforholdTjeneste.AktivitetAvtale(DatoIntervallEntitet.fraOgMedTilOgMed(fraDato, tilDato), BigDecimal.valueOf(stillingsprosent));
    }

    private static ArbeidsforholdTjeneste.Permisjon permisjon(LocalDate fraDato, LocalDate tilDato, int permisjonsprosent) {
        return new ArbeidsforholdTjeneste.Permisjon(DatoIntervallEntitet.fraOgMedTilOgMed(fraDato, tilDato), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(permisjonsprosent));
    }
}
