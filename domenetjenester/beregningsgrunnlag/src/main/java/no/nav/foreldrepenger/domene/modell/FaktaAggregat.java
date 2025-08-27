package no.nav.foreldrepenger.domene.modell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class FaktaAggregat {

    private List<FaktaArbeidsforhold> faktaArbeidsforholdListe = new ArrayList<>();
    private FaktaAktør faktaAktør;

    private FaktaAggregat() {
    }

    public FaktaAggregat(FaktaAggregat faktaAggregatDto) {
        this.faktaArbeidsforholdListe = faktaAggregatDto.getFaktaArbeidsforhold().stream().map(FaktaArbeidsforhold::new).toList();
        this.faktaAktør = faktaAggregatDto.getFaktaAktør().map(FaktaAktør::new).orElse(null);
    }

    public List<FaktaArbeidsforhold> getFaktaArbeidsforhold() {
        return faktaArbeidsforholdListe.stream().toList();
    }

    public Optional<FaktaArbeidsforhold> getFaktaArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforholdDto) {
        return faktaArbeidsforholdListe.stream()
            .filter(fa -> fa.gjelderFor(bgAndelArbeidsforholdDto.getArbeidsgiver(), bgAndelArbeidsforholdDto.getArbeidsforholdRef()))
            .findFirst();
    }

    public Optional<FaktaArbeidsforhold> getFaktaArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (andel.getBgAndelArbeidsforhold().isEmpty()) {
            return Optional.empty();
        }
        return faktaArbeidsforholdListe.stream()
            .filter(fa -> fa.gjelderFor(andel.getBgAndelArbeidsforhold().get().getArbeidsgiver(),
                andel.getBgAndelArbeidsforhold().get().getArbeidsforholdRef()))
            .findFirst();
    }


    public Optional<FaktaAktør> getFaktaAktør() {
        return Optional.ofNullable(faktaAktør);
    }

    private void leggTilFaktaArbeidsforholdOgErstattEksisterende(FaktaArbeidsforhold faktaArbeidsforhold) {
        var eksisterende = this.faktaArbeidsforholdListe.stream()
            .filter(fa -> fa.gjelderFor(faktaArbeidsforhold.getArbeidsgiver(), faktaArbeidsforhold.getArbeidsforholdRef()))
            .findFirst();
        eksisterende.ifPresent(this.faktaArbeidsforholdListe::remove);
        this.faktaArbeidsforholdListe.add(faktaArbeidsforhold);
    }

    private void leggTilFaktaArbeidsforholdOgKopierEksisterende(FaktaArbeidsforhold faktaArbeidsforhold) {
        var eksisterende = this.faktaArbeidsforholdListe.stream()
            .filter(fa -> fa.gjelderFor(faktaArbeidsforhold.getArbeidsgiver(), faktaArbeidsforhold.getArbeidsforholdRef()))
            .findFirst();
        eksisterende.ifPresentOrElse(kopier(faktaArbeidsforhold), () -> this.faktaArbeidsforholdListe.add(faktaArbeidsforhold));
    }

    private Consumer<FaktaArbeidsforhold> kopier(FaktaArbeidsforhold faktaArbeidsforhold) {
        return e -> {
            var faktaBuilder = FaktaArbeidsforhold.builder(e);
            kopierVurdering(faktaBuilder::medErTidsbegrenset, faktaArbeidsforhold.getErTidsbegrenset());
            kopierVurdering(faktaBuilder::medHarMottattYtelse, faktaArbeidsforhold.getHarMottattYtelse());
            kopierVurdering(faktaBuilder::medHarLønnsendringIBeregningsperioden, faktaArbeidsforhold.getHarLønnsendringIBeregningsperioden());
        };
    }

    private void kopierVurdering(Function<FaktaVurdering, FaktaArbeidsforhold.Builder> builderFunction, FaktaVurdering vurdering) {
        if (vurdering != null && vurdering.getVurdering() != null) {
            builderFunction.apply(vurdering);
        }
    }


    void setFaktaAktør(FaktaAktør faktaAktør) {
        this.faktaAktør = faktaAktør;
    }

    @Override
    public String toString() {
        return "FaktaAggregat{" + "faktaArbeidsforholdListe=" + faktaArbeidsforholdListe + ", faktaAktør=" + faktaAktør + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FaktaAggregat kladd;

        private Builder() {
            kladd = new FaktaAggregat();
        }

        private Builder(FaktaAggregat faktaAggregatDto) {
            kladd = new FaktaAggregat(faktaAggregatDto);
        }

        static Builder oppdater(FaktaAggregat faktaAggregatDto) {
            return new FaktaAggregat.Builder(faktaAggregatDto);
        }

        public FaktaAktør.Builder getFaktaAktørBuilder() {
            return kladd.getFaktaAktør().map(FaktaAktør.Builder::oppdater).orElse(FaktaAktør.builder());
        }

        public FaktaArbeidsforhold.Builder getFaktaArbeidsforholdBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            return kladd.faktaArbeidsforholdListe.stream()
                .filter(f -> f.gjelderFor(arbeidsgiver, arbeidsforholdRef))
                .findFirst()
                .map(FaktaArbeidsforhold.Builder::oppdater)
                .orElse(new FaktaArbeidsforhold.Builder(arbeidsgiver, arbeidsforholdRef));
        }

        public Builder erstattEksisterendeEllerLeggTil(FaktaArbeidsforhold faktaArbeidsforhold) {
            kladd.leggTilFaktaArbeidsforholdOgErstattEksisterende(faktaArbeidsforhold);
            return this;
        }

        public Builder medFaktaAktør(FaktaAktør faktaAktør) {
            kladd.setFaktaAktør(faktaAktør);
            return this;
        }

        public FaktaAggregat build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            if (manglerFakta()) {
                throw new IllegalStateException("Må ha satt enten faktaArbeidsforhold eller faktaAktør");
            }
        }

        // Brukes i fp-sak
        public boolean manglerFakta() {
            return kladd.faktaArbeidsforholdListe.isEmpty() && kladd.faktaAktør == null;
        }

    }
}
