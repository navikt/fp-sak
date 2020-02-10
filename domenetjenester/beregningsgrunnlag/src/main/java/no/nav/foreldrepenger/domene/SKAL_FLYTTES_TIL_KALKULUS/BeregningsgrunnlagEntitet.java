package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;


import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType.BRUKERS_STATUS;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType.PERIODISERING;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
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


    @OneToMany(mappedBy = "beregningsgrunnlag")
    private List<SammenligningsgrunnlagPrStatus> sammenligningsgrunnlagPrStatusListe = new ArrayList<>();


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @MapKey(name = "regelType")
    private Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> regelSporingMap = new HashMap<>();

    @Lob
    @Column(name = "regellogg_skjaringstidspunkt")
    private String regelloggSkjæringstidspunkt;

    @Lob
    @Column(name = "regellogg_brukers_status")
    private String regelloggBrukersStatus;

    @Lob
    @Column(name = "regelinput_skjaringstidspunkt")
    private String regelInputSkjæringstidspunkt;

    @Lob
    @Column(name = "regelinput_brukers_status")
    private String regelInputBrukersStatus;

    @Lob
    @Column(name = "regelinput_periodisering")
    private String regelinputPeriodisering;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "grunnbeloep")))
    @ChangeTracked
    private Beløp grunnbeløp;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsgrunnlag", cascade = CascadeType.PERSIST)
    private List<BeregningsgrunnlagFaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = new ArrayList<>();

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt", nullable = false)
    private boolean overstyrt = false;

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
                .collect(Collectors.toUnmodifiableList());
    }

    public Sammenligningsgrunnlag getSammenligningsgrunnlag() {
        return sammenligningsgrunnlag;
    }

    public Beløp getGrunnbeløp() {
        return grunnbeløp;
    }

    public void leggTilBeregningsgrunnlagAktivitetStatus(BeregningsgrunnlagAktivitetStatus bgAktivitetStatus) {
        Objects.requireNonNull(bgAktivitetStatus, "beregningsgrunnlagAktivitetStatus");
        aktivitetStatuser.remove(bgAktivitetStatus); // NOSONAR
        aktivitetStatuser.add(bgAktivitetStatus);
    }

    public void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        Objects.requireNonNull(bgPeriode, "beregningsgrunnlagPeriode");
        if (!beregningsgrunnlagPerioder.contains(bgPeriode)) { // NOSONAR
            beregningsgrunnlagPerioder.add(bgPeriode);
        }
    }

    public String getRegelinputPeriodisering() {
        return regelSporingMap.containsKey(PERIODISERING) ? regelSporingMap.get(PERIODISERING).getRegelInput() : regelinputPeriodisering;
    }

    public String getRegelInputSkjæringstidspunkt() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelInput() : regelInputSkjæringstidspunkt;
    }

    public String getRegelloggSkjæringstidspunkt() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelEvaluering() : regelloggSkjæringstidspunkt;
    }

    public String getRegelInputBrukersStatus() {
        return regelSporingMap.containsKey(SKJÆRINGSTIDSPUNKT) ? regelSporingMap.get(SKJÆRINGSTIDSPUNKT).getRegelInput() : regelInputBrukersStatus;
    }

    public String getRegelloggBrukersStatus() {
        return regelSporingMap.containsKey(BRUKERS_STATUS) ? regelSporingMap.get(BRUKERS_STATUS).getRegelEvaluering() : regelloggBrukersStatus;
    }

    public Hjemmel getHjemmel() {
        if (aktivitetStatuser.isEmpty()) {
            return Hjemmel.UDEFINERT;
        }
        if (aktivitetStatuser.size() == 1) {
            return aktivitetStatuser.get(0).getHjemmel();
        }
        Optional<BeregningsgrunnlagAktivitetStatus> dagpenger = aktivitetStatuser.stream()
                .filter(as -> Hjemmel.F_14_7_8_49.equals(as.getHjemmel()))
                .findFirst();
        if (dagpenger.isPresent()) {
            return dagpenger.get().getHjemmel();
        }
        Optional<BeregningsgrunnlagAktivitetStatus> gjelder = aktivitetStatuser.stream()
                .filter(as -> !Hjemmel.F_14_7.equals(as.getHjemmel()))
                .findFirst();
        return gjelder.isPresent() ? gjelder.get().getHjemmel() : Hjemmel.F_14_7;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller
                .stream()
                .map(BeregningsgrunnlagFaktaOmBeregningTilfelle::getFaktaOmBeregningTilfelle)
                .collect(Collectors.toUnmodifiableList());
    }

    public List<SammenligningsgrunnlagPrStatus> getSammenligningsgrunnlagPrStatusListe() {
        return sammenligningsgrunnlagPrStatusListe;
    }

    public boolean isOverstyrt() {
        return overstyrt;
    }

    public BeregningsgrunnlagEntitet dypKopi() {
        // FIXME(OleSandbu) : her settes kopi på builder og modifiseres direkte.  I tillegg kopieres for mye.
        // Kan det skrives om?
        return Kopimaskin.deepCopy(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagEntitet)) {
            return false;
        }
        BeregningsgrunnlagEntitet other = (BeregningsgrunnlagEntitet) obj;
        return Objects.equals(this.getSkjæringstidspunkt(), other.getSkjæringstidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "skjæringstidspunkt=" + skjæringstidspunkt + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "grunnbeløp=" + grunnbeløp + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagEntitet original) {
        return new Builder(original);
    }

    void leggTilBeregningsgrunnlagRegel(BeregningsgrunnlagRegelSporing beregningsgrunnlagRegelSporing) {
        Objects.requireNonNull(beregningsgrunnlagRegelSporing, "beregningsgrunnlagRegelSporing");
        regelSporingMap.put(beregningsgrunnlagRegelSporing.getRegelType(), beregningsgrunnlagRegelSporing);
    }

    public static class Builder {
        private boolean built;
        private BeregningsgrunnlagEntitet kladd;

        private Builder() {
            kladd = new BeregningsgrunnlagEntitet();
        }

        private Builder(BeregningsgrunnlagEntitet original) {
            kladd = original;
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

        public Builder fjernAllePerioder() {
            verifiserKanModifisere();
            kladd.beregningsgrunnlagPerioder = new ArrayList<>();
            return this;
        }

        public Builder fjernAktivitetstatus(AktivitetStatus status) {
            verifiserKanModifisere();
            List<BeregningsgrunnlagAktivitetStatus> statuserSomSkalFjernes = kladd.aktivitetStatuser.stream().filter(a -> Objects.equals(a.getAktivitetStatus(), status)).collect(Collectors.toList());
            if (statuserSomSkalFjernes.size() != 1) {
                throw new IllegalStateException("Ikke entydig hvilken status som skal fjernes fra beregningsgrunnlaget.");
            }
            kladd.aktivitetStatuser.remove(statuserSomSkalFjernes.get(0));
            return this;
        }

        public Builder leggTilFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
            verifiserKanModifisere();
            faktaOmBeregningTilfeller.forEach(this::leggTilFaktaOmBeregningTilfeller);
            return this;
        }

        private void leggTilFaktaOmBeregningTilfeller(FaktaOmBeregningTilfelle tilfelle) {
            verifiserKanModifisere();
            BeregningsgrunnlagFaktaOmBeregningTilfelle b = BeregningsgrunnlagFaktaOmBeregningTilfelle.builder().medFaktaOmBeregningTilfelle(tilfelle).build(kladd);
            this.kladd.faktaOmBeregningTilfeller.add(b);
        }

        /**
         * @deprecated bruk -> {@link SammenligningsgrunnlagPrStatus}
         */
        @Deprecated
        public Builder medSammenligningsgrunnlagOld(Sammenligningsgrunnlag sammenligningsgrunnlag) {
            verifiserKanModifisere();
            sammenligningsgrunnlag.setBeregningsgrunnlag(kladd);
            kladd.sammenligningsgrunnlag = sammenligningsgrunnlag;
            return this;
        }

        public Builder leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusBuilder) { // NOSONAR
            kladd.sammenligningsgrunnlagPrStatusListe.add(sammenligningsgrunnlagPrStatusBuilder.medBeregningsgrunnlag(kladd).build());
            return this;
        }

        public Builder medRegelloggSkjæringstidspunkt(String regelInput, String regelEvaluering) {
            verifiserKanModifisere();
            // Lagrer til begge regel-sporinger til vi har kjørt migrering
            BeregningsgrunnlagRegelSporing.ny()
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering)
                .medRegelType(SKJÆRINGSTIDSPUNKT)
                .build(kladd);
            kladd.regelInputSkjæringstidspunkt = regelInput;
            kladd.regelloggSkjæringstidspunkt = regelEvaluering;
            return this;
        }

        public Builder medRegelloggBrukersStatus(String regelInput, String regelEvaluering) {
            verifiserKanModifisere();
            // Lagrer til begge regel-sporinger til vi har kjørt migrering
            BeregningsgrunnlagRegelSporing.ny()
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering)
                .medRegelType(BRUKERS_STATUS)
                .build(kladd);
            kladd.regelInputBrukersStatus = regelInput;
            kladd.regelloggBrukersStatus = regelEvaluering;
            return this;
        }

        public Builder medRegelinputPeriodisering(String regelInput) {
            verifiserKanModifisere();
          // Lagrer til begge regel-sporinger til vi har kjørt migrering
            if (regelInput != null) {
                BeregningsgrunnlagRegelSporing.ny()
                    .medRegelInput(regelInput)
                    .medRegelType(PERIODISERING)
                    .build(kladd);
            }
            kladd.regelinputPeriodisering = regelInput;
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
            if(built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(kladd.skjæringstidspunkt, "skjæringstidspunkt");
        }
    }
}
