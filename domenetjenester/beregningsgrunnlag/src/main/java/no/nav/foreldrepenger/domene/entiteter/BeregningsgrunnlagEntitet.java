package no.nav.foreldrepenger.domene.entiteter;


import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.BRUKERS_STATUS;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.PERIODISERING;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Beregningsgrunnlag")
@Table(name = "BEREGNINGSGRUNNLAG")
public class BeregningsgrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEREGNINGSGRUNNLAG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "skjaringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = new ArrayList<>();

    @OneToOne(mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private Sammenligningsgrunnlag sammenligningsgrunnlag;

    @OneToOne(mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private BesteberegninggrunnlagEntitet besteberegninggrunnlag;

    @OneToMany(mappedBy = "beregningsgrunnlag")
    private List<SammenligningsgrunnlagPrStatus> sammenligningsgrunnlagPrStatusListe = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @MapKey(name = "regelType")
    private Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> regelSporingMap = new EnumMap<>(BeregningsgrunnlagRegelType.class);

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "grunnbeloep"))
    @ChangeTracked
    private Beløp grunnbeløp;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private List<BeregningsgrunnlagFaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = new ArrayList<>();

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt", nullable = false)
    private boolean overstyrt = false;

    public BeregningsgrunnlagEntitet(BeregningsgrunnlagEntitet kopi) {
        this.grunnbeløp = kopi.getGrunnbeløp();
        this.overstyrt = kopi.isOverstyrt();
        this.skjæringstidspunkt = kopi.getSkjæringstidspunkt();
        kopi.getSammenligningsgrunnlag().map(Sammenligningsgrunnlag::new).ifPresent(this::setSammenligningsgrunnlag);
        kopi.getBesteberegninggrunnlag().map(BesteberegninggrunnlagEntitet::new).ifPresent(this::setBesteberegninggrunnlag);

        kopi.getRegelSporinger().values().stream().map(BeregningsgrunnlagRegelSporing::new)
            .forEach(this::leggTilBeregningsgrunnlagRegel);
        kopi.getSammenligningsgrunnlagPrStatusListe().stream().map(SammenligningsgrunnlagPrStatus::new).forEach(this::leggTilSammenligningsgrunnlagPrStatus);
        kopi.faktaOmBeregningTilfeller.stream().map(BeregningsgrunnlagFaktaOmBeregningTilfelle::new).forEach(this::leggTilFaktaOmBeregningTilfelle);
        kopi.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::new).forEach(this::leggTilBeregningsgrunnlagAktivitetStatus);
        kopi.getBeregningsgrunnlagPerioder().stream().map(BeregningsgrunnlagPeriode::new)
            .forEach(this::leggTilBeregningsgrunnlagPeriode);
    }

    public BeregningsgrunnlagEntitet() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public List<BeregningsgrunnlagAktivitetStatus> getAktivitetStatuser() {
        return Collections.unmodifiableList(aktivitetStatuser);
    }

    public List<BeregningsgrunnlagPeriode> getBeregningsgrunnlagPerioder() {
        return beregningsgrunnlagPerioder
            .stream()
            .sorted(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
            .toList();
    }

    public Optional<Sammenligningsgrunnlag> getSammenligningsgrunnlag() {
        return Optional.ofNullable(sammenligningsgrunnlag);
    }

    public Optional<BesteberegninggrunnlagEntitet> getBesteberegninggrunnlag() {
        return Optional.ofNullable(besteberegninggrunnlag);
    }

    public Beløp getGrunnbeløp() {
        return grunnbeløp;
    }

    public void leggTilBeregningsgrunnlagAktivitetStatus(BeregningsgrunnlagAktivitetStatus bgAktivitetStatus) {
        Objects.requireNonNull(bgAktivitetStatus, "beregningsgrunnlagAktivitetStatus");
        bgAktivitetStatus.setBeregningsgrunnlag(this);
        // Aktivitetstatuser burde implementeres som eit Set
        if (!aktivitetStatuser.contains(bgAktivitetStatus)) {
            aktivitetStatuser.add(bgAktivitetStatus);
        }
    }

    public void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        Objects.requireNonNull(bgPeriode, "beregningsgrunnlagPeriode");
        if (!beregningsgrunnlagPerioder.contains(bgPeriode)) {
            bgPeriode.setBeregningsgrunnlag(this);
            beregningsgrunnlagPerioder.add(bgPeriode);
        }
    }

    public void leggTilFaktaOmBeregningTilfelle(BeregningsgrunnlagFaktaOmBeregningTilfelle beregningsgrunnlagFaktaOmBeregningTilfelle) {
        Objects.requireNonNull(beregningsgrunnlagFaktaOmBeregningTilfelle, "beregningsgrunnlagFaktaOmBeregningTilfelle");
        // Aktivitetstatuser burde implementeres som eit Set
        if (!faktaOmBeregningTilfeller.contains(beregningsgrunnlagFaktaOmBeregningTilfelle)) {
            beregningsgrunnlagFaktaOmBeregningTilfelle.setBeregningsgrunnlag(this);
            faktaOmBeregningTilfeller.add(beregningsgrunnlagFaktaOmBeregningTilfelle);
        }
    }

    public void leggTilSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        Objects.requireNonNull(sammenligningsgrunnlagPrStatus, "sammenligningsgrunnlagPrStatus");
        var finnesFraFør = sammenligningsgrunnlagPrStatusListe.stream()
            .anyMatch(sg -> sg.getSammenligningsgrunnlagType().equals(sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType()));
        if (finnesFraFør) {
            throw new IllegalStateException("Feil: Sammenligningsgrunnlag med type "
                + sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType() + " finnes allerede på grunnlaget");
        }
        // Aktivitetstatuser burde implementeres som eit Set
        if (!sammenligningsgrunnlagPrStatusListe.contains(sammenligningsgrunnlagPrStatus)) {
            sammenligningsgrunnlagPrStatus.setBeregningsgrunnlag(this);
            sammenligningsgrunnlagPrStatusListe.add(sammenligningsgrunnlagPrStatus);
        } else {
            throw new IllegalArgumentException("Kan ikke legge til sammenligningsgrunnlag for " + sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType() +
                " fordi det allerede er lagt til.");
        }
    }

    void setSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag) {
        sammenligningsgrunnlag.setBeregningsgrunnlag(this);
        this.sammenligningsgrunnlag = sammenligningsgrunnlag;
    }

    void setBesteberegninggrunnlag(BesteberegninggrunnlagEntitet besteberegninggrunnlag) {
        besteberegninggrunnlag.setBeregningsgrunnlag(this);
        this.besteberegninggrunnlag = besteberegninggrunnlag;
    }

    public BeregningsgrunnlagRegelSporing getRegelsporing(BeregningsgrunnlagRegelType regelType) {
        return regelSporingMap.getOrDefault(regelType, null);
    }

    public String getRegelinputPeriodisering() {
        return regelSporingMap.containsKey(PERIODISERING) ? regelSporingMap.get(PERIODISERING).getRegelInput() : null;
    }

    public String getRegelInputSkjæringstidspunkt() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelInput() : null;
    }

    public String getRegelloggSkjæringstidspunkt() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelEvaluering() : null;
    }

    public String getRegelInputBrukersStatus() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelInput() : null;
    }

    public String getRegelloggBrukersStatus() {
        return regelSporingMap.containsKey(BRUKERS_STATUS) ? regelSporingMap.get(BRUKERS_STATUS).getRegelEvaluering() : null;
    }


    public Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> getRegelSporinger() {
        return regelSporingMap;
    }

    public Hjemmel getHjemmel() {
        if (aktivitetStatuser.isEmpty()) {
            return Hjemmel.UDEFINERT;
        }
        if (aktivitetStatuser.size() == 1) {
            return aktivitetStatuser.get(0).getHjemmel();
        }
        var dagpenger = aktivitetStatuser.stream()
            .filter(as -> Hjemmel.F_14_7_8_49.equals(as.getHjemmel()))
            .findFirst();
        if (dagpenger.isPresent()) {
            return dagpenger.get().getHjemmel();
        }
        var gjelder = aktivitetStatuser.stream()
            .filter(as -> !Hjemmel.F_14_7.equals(as.getHjemmel()))
            .findFirst();
        return gjelder.isPresent() ? gjelder.get().getHjemmel() : Hjemmel.F_14_7;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller
            .stream()
            .map(BeregningsgrunnlagFaktaOmBeregningTilfelle::getFaktaOmBeregningTilfelle)
            .toList();
    }

    public List<SammenligningsgrunnlagPrStatus> getSammenligningsgrunnlagPrStatusListe() {
        return sammenligningsgrunnlagPrStatusListe;
    }

    public boolean isOverstyrt() {
        return overstyrt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagEntitet other)) {
            return false;
        }
        return Objects.equals(this.getSkjæringstidspunkt(), other.getSkjæringstidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id + ", "
            + "skjæringstidspunkt=" + skjæringstidspunkt + ", "
            + "grunnbeløp=" + grunnbeløp + ", "
            + ">";
    }

    public static Builder ny() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagEntitet original) {
        return new Builder(original, false);
    }

    void leggTilBeregningsgrunnlagRegel(BeregningsgrunnlagRegelSporing beregningsgrunnlagRegelSporing) {
        Objects.requireNonNull(beregningsgrunnlagRegelSporing, "beregningsgrunnlagRegelSporing");
        beregningsgrunnlagRegelSporing.setBeregningsgrunnlag(this);
        regelSporingMap.put(beregningsgrunnlagRegelSporing.getRegelType(), beregningsgrunnlagRegelSporing);
    }

    public static class Builder {
        private boolean built;
        private BeregningsgrunnlagEntitet kladd;

        private Builder() {
            kladd = new BeregningsgrunnlagEntitet();
        }

        private Builder(BeregningsgrunnlagEntitet original, boolean erOppdatering) {
            if (erOppdatering) {
                kladd = original;
            } else {
                kladd = new BeregningsgrunnlagEntitet(original);
            }
        }

        static Builder oppdater(Optional<BeregningsgrunnlagEntitet> original) {
            if (original.isEmpty()) {
                return new Builder();
            }
            return oppdater(original.get());
        }

        public static Builder oppdater(BeregningsgrunnlagEntitet original) {
            if (original.getId() != null) {
                throw new IllegalStateException("Kan ikke endre på et lagret grunnlag");
            }
            return new Builder(original, true);
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            verifiserKanModifisere();
            kladd.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medGrunnbeløp(BigDecimal grunnbeløp) {
            verifiserKanModifisere();
            kladd.grunnbeløp = new Beløp(grunnbeløp);
            return this;
        }

        public Builder medGrunnbeløp(Beløp grunnbeløp) {
            verifiserKanModifisere();
            kladd.grunnbeløp = grunnbeløp;
            return this;
        }

        public Builder leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.Builder aktivitetStatusBuilder) {
            verifiserKanModifisere();
            aktivitetStatusBuilder.build(kladd);
            return this;
        }

        public Builder leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder) {
            verifiserKanModifisere();
            beregningsgrunnlagPeriodeBuilder.build(kladd);
            return this;
        }

        public Builder leggTilFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
            verifiserKanModifisere();
            faktaOmBeregningTilfeller.forEach(tilfelle -> BeregningsgrunnlagFaktaOmBeregningTilfelle.builder().medFaktaOmBeregningTilfelle(tilfelle).build(kladd));
            return this;
        }

        /**
         * @deprecated bruk -> {@link SammenligningsgrunnlagPrStatus}
         */
        @Deprecated
        public Builder medSammenligningsgrunnlagOld(Sammenligningsgrunnlag sammenligningsgrunnlag) {
            verifiserKanModifisere();
            kladd.setSammenligningsgrunnlag(sammenligningsgrunnlag);
            return this;
        }


        public Builder medBesteberegninggrunnlag(BesteberegninggrunnlagEntitet besteberegninggrunnlag) {
            verifiserKanModifisere();
            if (besteberegninggrunnlag == null) {
                return this;
            }
            kladd.setBesteberegninggrunnlag(besteberegninggrunnlag);
            return this;
        }


        public Builder leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusBuilder) {
            sammenligningsgrunnlagPrStatusBuilder.build(kladd);
            return this;
        }

        public Builder medRegelSporing(String regelInput, String regelEvaluering, BeregningsgrunnlagRegelType regelType, String regelVersjon) {
            verifiserKanModifisere();
            if (regelInput != null) {
                BeregningsgrunnlagRegelSporing.ny()
                    .medRegelInput(regelInput)
                    .medRegelEvaluering(regelEvaluering)
                    .medRegelVersjon(regelVersjon)
                    .medRegelType(regelType)
                    .build(kladd);
            }
            return this;
        }

        public Builder medOverstyring(boolean overstyrt) {
            verifiserKanModifisere();
            kladd.overstyrt = overstyrt;
            return this;
        }

        public BeregningsgrunnlagEntitet build() {
            verifyStateForBuild();
            built = true;
            return kladd;
        }

        private void verifiserKanModifisere() {
            if (built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(kladd.skjæringstidspunkt, "skjæringstidspunkt");
        }

        public List<BeregningsgrunnlagPeriode.Builder> getPeriodeBuilders(ÅpenDatoIntervallEntitet periode) {
            return kladd.getBeregningsgrunnlagPerioder().stream().filter(p -> p.getPeriode().overlapper(periode))
                .map(BeregningsgrunnlagPeriode::oppdater).toList();
        }
    }
}
