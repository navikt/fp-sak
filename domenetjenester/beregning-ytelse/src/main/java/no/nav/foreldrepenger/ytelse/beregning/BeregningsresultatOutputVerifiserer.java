package no.nav.foreldrepenger.ytelse.beregning;

import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.vedtak.exception.TekniskException;

final class BeregningsresultatOutputVerifiserer {

    private BeregningsresultatOutputVerifiserer() {
    }

    static void verifiserOutput(BeregningsresultatEntitet beregningsresultat) {
        Objects.requireNonNull(beregningsresultat, "Beregningsresultat");
        Objects.requireNonNull(beregningsresultat.getRegelInput(), "Regelinput beregningsresultat");
        Objects.requireNonNull(beregningsresultat.getRegelSporing(), "Regelsporing beregningsresultat");
        verifiserPerioder(beregningsresultat.getBeregningsresultatPerioder());
    }

    private static void verifiserPerioder(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        beregningsresultatPerioder.forEach(BeregningsresultatOutputVerifiserer::verifiserPeriode);
    }

    private static void verifiserPeriode(BeregningsresultatPeriode periode) {
        Objects.requireNonNull(periode, "beregningsresultatperiode");
        Objects.requireNonNull(periode.getBeregningsresultatPeriodeFom(), "beregningsresultatperiodeFom");
        Objects.requireNonNull(periode.getBeregningsresultatPeriodeTom(), "beregningsresultatperiodeTom");
        verifiserDagsats(periode.getDagsats(), "beregningsresultatperiode");
        verifiserAndeler(periode.getBeregningsresultatAndelList());
    }

    private static void verifiserAndeler(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        beregningsresultatAndelList.forEach(BeregningsresultatOutputVerifiserer::verifiserAndel);
    }

    private static void verifiserAndel(BeregningsresultatAndel andel) {
        Objects.requireNonNull(andel.getAktivitetStatus(), "beregningsresultatandelAktivitetstatus");
        Objects.requireNonNull(andel.getInntektskategori(), "beregningsresultatandelInntektskategori");

        verifiserDagsats(andel.getDagsats(), "beregningsresultatandel");

        if (!andel.erBrukerMottaker()) {
            var arbeidsgiver = andel.getArbeidsgiver()
                .orElseThrow(() -> new TekniskException("FP-370745",
                    "Postcondition feilet: Beregningsresultat i ugyldig "
                        + "tilstand etter steg. Dagsats på andel skal til arbeidsgiver men arbeidsgiver er ikke satt"));
            verifiserArbeidsgiver(arbeidsgiver);
        }

        if (!andel.getAktivitetStatus().erArbeidstaker() && !andel.erBrukerMottaker()) {
            var msg = String.format(
                "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Andel med status %s skal "
                    + "aldri ha utbetaling til arbeidsgiver", andel.getAktivitetStatus().getKode());
            throw new TekniskException("FP-370747", msg);
        }
    }

    private static void verifiserArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.erAktørId()) {
            Objects.requireNonNull(arbeidsgiver.getAktørId(), "aktørId");
        } else {
            Objects.requireNonNull(arbeidsgiver.getOrgnr(), "orgnr");
        }
    }

    private static void verifiserDagsats(int dagsats, String obj) {
        if (dagsats < 0) {
            var msg = String.format("Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. "
                + "Dagsatsen på %s er mindre enn 0, men skulle ikke vært det.", obj);
            throw new TekniskException("FP-370744", msg);
        }
    }

}
