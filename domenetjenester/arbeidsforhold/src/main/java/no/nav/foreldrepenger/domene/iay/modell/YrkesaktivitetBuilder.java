package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class YrkesaktivitetBuilder {
    private final Yrkesaktivitet kladd;
    private boolean oppdaterer;

    private YrkesaktivitetBuilder(Yrkesaktivitet kladd, boolean oppdaterer) {
        this.kladd = kladd;
        this.oppdaterer = oppdaterer;
    }

    static YrkesaktivitetBuilder ny() {
        return new YrkesaktivitetBuilder(new Yrkesaktivitet(), false);
    }

    static YrkesaktivitetBuilder oppdatere(Yrkesaktivitet oppdatere) {
        return new YrkesaktivitetBuilder(oppdatere, true);
    }

    public static YrkesaktivitetBuilder oppdatere(Optional<Yrkesaktivitet> oppdatere) {
        return oppdatere.map(YrkesaktivitetBuilder::oppdatere).orElseGet(YrkesaktivitetBuilder::ny);
    }

    public YrkesaktivitetBuilder medArbeidType(ArbeidType arbeidType) {
        kladd.setArbeidType(arbeidType);
        return this;
    }

    public YrkesaktivitetBuilder medArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
        kladd.setArbeidsforholdId(arbeidsforholdId);
        return this;
    }

    public YrkesaktivitetBuilder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        kladd.setArbeidsgiver(arbeidsgiver);
        return this;
    }

    public YrkesaktivitetBuilder medArbeidsgiverNavn(String arbeidsgiver) {
        kladd.setNavnArbeidsgiverUtland(arbeidsgiver);
        return this;
    }

    Yrkesaktivitet getKladd() {
        return kladd;
    }

    public PermisjonBuilder getPermisjonBuilder() {
        return nyPermisjonBuilder();
    }

    public YrkesaktivitetBuilder leggTilPermisjon(Permisjon permisjon) {
        kladd.leggTilPermisjon(permisjon);
        return this;
    }

    public YrkesaktivitetBuilder tilbakestillAvtaler() {
        kladd.tilbakestillAvtaler();
        return this;
    }

    public AktivitetsAvtaleBuilder getAktivitetsAvtaleBuilder() {
        return nyAktivitetsAvtaleBuilder();
    }

    public YrkesaktivitetBuilder leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder builder) {
        if (!builder.isOppdatering()) {
            var aktivitetsAvtale = builder.build();
            kladd.leggTilAktivitetsAvtale(aktivitetsAvtale);
        }
        return this;
    }

    public YrkesaktivitetBuilder migrerFraRegisterTilOverstyrt() {
        this.oppdaterer = false;
        return this;
    }

    public boolean getErOppdatering() {
        return this.oppdaterer;
    }

    public Yrkesaktivitet build() {
        return kladd;
    }

    public static AktivitetsAvtaleBuilder nyAktivitetsAvtaleBuilder() {
        return AktivitetsAvtaleBuilder.ny();
    }

    public static PermisjonBuilder nyPermisjonBuilder() {
        return PermisjonBuilder.ny();
    }

    public AktivitetsAvtaleBuilder getAktivitetsAvtaleBuilder(DatoIntervallEntitet aktivitetsPeriode, boolean erAnsettelsesperioden) {
        var oppdater = AktivitetsAvtaleBuilder.oppdater(kladd.getAlleAktivitetsAvtaler()
                .stream()
                .filter(
                    aa -> aa.matcherPeriode(aktivitetsPeriode) && (!kladd.erArbeidsforhold() || aa.erAnsettelsesPeriode() == erAnsettelsesperioden))
                .findFirst());
        oppdater.medPeriode(aktivitetsPeriode);
        return oppdater;
    }

    public boolean harIngenAvtaler() {
        return new YrkesaktivitetFilter(null, List.of(kladd)).getAktivitetsAvtalerForArbeid().isEmpty();
    }

    public boolean harIngenAnsettelsesPerioder() {
        return new YrkesaktivitetFilter(null, List.of(kladd)).getAnsettelsesPerioder().isEmpty();
    }

    /*
     * Kommer inn en bekreftet ansettelsesperiode som skulle være 1-1 med eksisterende - men det kan ha vært splitt/merge av perioder
     * Logikk: Fjerne alle ansettelsesperioder som er omfattet av aktivitetsperiode - enten hele ansettelsesperioden eller erstatte med deler som ikke overlapper
     */
    public void fjernAnsettelsesPeriode(DatoIntervallEntitet aktivitetsPeriode) {
        // Disse skal slettes til slutt
        var ansettelsesperioderSomOverlapper = kladd.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .filter(p -> aktivitetsPeriode.overlapper(p.getPeriode()))
            .collect(Collectors.toSet());
        var workingSet = new HashSet<>(ansettelsesperioderSomOverlapper);
        while (workingSet.stream().anyMatch(p -> aktivitetsPeriode.overlapper(p.getPeriode()))) {
            workingSet.removeAll(omsluttetAvAktivitetsperiode(workingSet, aktivitetsPeriode));
            var overlapper = workingSet.stream().filter(p -> aktivitetsPeriode.overlapper(p.getPeriode())).collect(Collectors.toSet());
            var oppdelt = overlapper.stream().map(p -> splitOverlappLR(p, aktivitetsPeriode)).flatMap(Collection::stream).collect(Collectors.toSet());
            workingSet.removeAll(overlapper);
            workingSet.addAll(oppdelt);
        }
        ansettelsesperioderSomOverlapper.stream().map(AktivitetsAvtale::getPeriode).forEach(kladd::fjernAnsettelsesPeriode);
        workingSet.forEach(kladd::leggTilAktivitetsAvtale);
    }

    private Collection<AktivitetsAvtale> omsluttetAvAktivitetsperiode(Set<AktivitetsAvtale> ansettelsesperioder, DatoIntervallEntitet aktivitetsPeriode) {
        return ansettelsesperioder.stream()
            .filter(p -> p.getPeriode().erOmsluttetAv(aktivitetsPeriode))
            .collect(Collectors.toSet());
    }

    private Collection<AktivitetsAvtale> splitOverlappLR(AktivitetsAvtale ansettelsesperiode, DatoIntervallEntitet aktivitetsPeriode) {
        var resultat = new HashSet<AktivitetsAvtale>();
        if (ansettelsesperiode.getPeriode().getFomDato().isBefore(aktivitetsPeriode.getFomDato())) {
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(ansettelsesperiode.getPeriode().getFomDato(), aktivitetsPeriode.getFomDato().minusDays(1L));
            resultat.add(nyAktivitetsAvtaleBuilder().medBeskrivelse(ansettelsesperiode.getBeskrivelse()).medPeriode(periode).build());
        }
        if (ansettelsesperiode.getPeriode().getTomDato().isAfter(aktivitetsPeriode.getTomDato())) {
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsPeriode.getTomDato().plusDays(1L), ansettelsesperiode.getPeriode().getTomDato());
            resultat.add(nyAktivitetsAvtaleBuilder().medBeskrivelse(ansettelsesperiode.getBeskrivelse()).medPeriode(periode).build());

        }
        return resultat;
    }

    public YrkesaktivitetBuilder medArbeidType(String kode) {
        return medArbeidType(ArbeidType.fraKode(kode));
    }

}
