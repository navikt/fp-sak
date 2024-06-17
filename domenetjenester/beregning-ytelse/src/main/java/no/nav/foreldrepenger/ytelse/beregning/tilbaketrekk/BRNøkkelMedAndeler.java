package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BRNøkkelMedAndeler {
    private AktivitetOgArbeidsgiverNøkkel nøkkel;
    private List<BeregningsresultatAndel> andelerTilknyttetNøkkel = new ArrayList<>();

    public BRNøkkelMedAndeler(AktivitetOgArbeidsgiverNøkkel nøkkel) {
        this.nøkkel = nøkkel;
    }

    public AktivitetOgArbeidsgiverNøkkel getNøkkel() {
        return nøkkel;
    }

    public List<BeregningsresultatAndel> getAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel;
    }

    public List<InternArbeidsforholdRef> getAlleReferanserForDenneNøkkelen() {
        return andelerTilknyttetNøkkel.stream().map(BeregningsresultatAndel::getArbeidsforholdRef).toList();
    }

    public List<BeregningsresultatAndel> getBrukersAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel.stream().filter(BeregningsresultatAndel::erBrukerMottaker).toList();
    }

    public List<BeregningsresultatAndel> getArbeidsgiversAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel.stream().filter(a -> !a.erBrukerMottaker()).toList();
    }

    public List<BeregningsresultatAndel> getAndelerSomHarReferanse() {
        return andelerTilknyttetNøkkel.stream().filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).toList();
    }

    public Optional<BeregningsresultatAndel> getBrukersAndelMedReferanse(InternArbeidsforholdRef ref) {
        var korresponderendeAndeler = getBrukersAndelerTilknyttetNøkkel().stream()
            .filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref))
            .toList();
        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException(
                "Forventet å finne maks en korresponderende BeregningsresultatAndel " + nøkkel + ". Antall matchende aktiviteter var "
                    + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getArbeidsgiversAndelMedReferanse(InternArbeidsforholdRef ref) {
        var korresponderendeAndeler = getArbeidsgiversAndelerTilknyttetNøkkel().stream()
            .filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref))
            .toList();
        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException(
                "Forventet å finne maks en korresponderende BeregningsresultatAndel " + nøkkel + ". Antall matchende aktiviteter var "
                    + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getBrukersAndelUtenreferanse() {
        var korresponderendeAndeler = getBrukersAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .toList();

        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException(
                "Forventet å finne maks en andel uten referanse for BeregningsresultatAndel " + nøkkel + ". Antall matchende andeler var "
                    + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getArbeidsgiversAndelUtenReferanse() {
        var korresponderendeAndeler = getArbeidsgiversAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .toList();

        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException(
                "Forventet å finne maks en andel uten referanse for BeregningsresultatAndel " + nøkkel + ". Antall matchende andeler var "
                    + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    /**
     * Etter automatisk omfordeling (blant annet) kan vi ha flere like andeler med samme ag og ref.
     * Her må alle returneres. Håndteres manuelt av SBH intill https://jira.adeo.no/browse/TFP-2709 er løst.
     */
    public List<BeregningsresultatAndel> getAlleBrukersAndelerMedReferanse(InternArbeidsforholdRef ref) {
        return getBrukersAndelerTilknyttetNøkkel().stream().filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref)).toList();
    }

    /**
     * Etter automatisk omfordeling (blant annet) kan vi ha flere like andeler med samme ag og ref.
     * Her må alle returneres. Håndteres manuelt av SBH intill https://jira.adeo.no/browse/TFP-2709 er løst.
     *
     * @return
     */
    public List<BeregningsresultatAndel> getAlleBrukersAndelerUtenReferanse() {
        return getBrukersAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .toList();
    }

    /**
     * Etter automatisk omfordeling (blant annet) kan vi ha flere like andeler med samme ag og ref.
     * Her må alle returneres. Håndteres manuelt av SBH intill https://jira.adeo.no/browse/TFP-2709 er løst.
     */
    public List<BeregningsresultatAndel> getAlleArbeidsgiversAndelerMedReferanse(InternArbeidsforholdRef ref) {
        return getArbeidsgiversAndelerTilknyttetNøkkel().stream().filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref)).toList();
    }

    /**
     * Etter automatisk omfordeling (blant annet) kan vi ha flere like andeler med samme ag og ref.
     * Her må alle returneres. Håndteres manuelt av SBH intill https://jira.adeo.no/browse/TFP-2709 er løst.
     */
    public List<BeregningsresultatAndel> getAlleArbeidsgiversAndelerUtenReferanse() {
        return getArbeidsgiversAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .toList();
    }

    void leggTilAndel(BeregningsresultatAndel andel) {
        if (matcherNøkkel(andel)) {
            andelerTilknyttetNøkkel.add(andel);
        }
    }

    private boolean matcherNøkkel(BeregningsresultatAndel andel) {
        return Objects.equals(andel.getAktivitetOgArbeidsgiverNøkkel(), nøkkel);
    }

    public List<BeregningsresultatAndel> getAlleAndelerMedRefSomIkkeFinnesIListe(List<InternArbeidsforholdRef> referanseliste) {
        return andelerTilknyttetNøkkel.stream().filter(a -> !referanseliste.contains(a.getArbeidsforholdRef())).toList();
    }

    public boolean erArbeidstaker() {
        return nøkkel.erArbeidstaker();
    }

    public List<BeregningsresultatAndel> getAlleAndeler() {
        return andelerTilknyttetNøkkel;
    }

    public List<BeregningsresultatAndel> getAndelerUtenReferanse() {
        return andelerTilknyttetNøkkel.stream().filter(a -> !a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).toList();
    }
}
