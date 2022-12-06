package no.nav.foreldrepenger.domene.entiteter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "SammenligningsgrunnlagPrStatus")
@Table(name = "BG_SG_PR_STATUS")
public class SammenligningsgrunnlagPrStatus extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_SG_PR_STATUS")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "sammenligningsperiode_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "sammenligningsperiode_tom"))
    })
    private DatoIntervallEntitet sammenligningsperiode;

    @Convert(converter = SammenligningsgrunnlagType.KodeverdiConverter.class)
    @Column(name="sammenligningsgrunnlag_type", nullable = false)
    private SammenligningsgrunnlagType sammenligningsgrunnlagType;

    @Column(name = "rapportert_pr_aar", nullable = false)
    private BigDecimal rapportertPrÅr;

    @Column(name = "avvik_promille", nullable = false)
    private BigDecimal avvikPromille = BigDecimal.ZERO;

    @JsonBackReference
    @OneToOne(optional = false)
    @JoinColumn(name = "beregningsgrunnlag_id", nullable = false, updatable = false, unique = true)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    public SammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        this.avvikPromille = sammenligningsgrunnlagPrStatus.getAvvikPromille();
        this.rapportertPrÅr = sammenligningsgrunnlagPrStatus.getRapportertPrÅr();
        this.sammenligningsgrunnlagType = sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType();
        this.sammenligningsperiode = sammenligningsgrunnlagPrStatus.sammenligningsperiode;
    }

    public SammenligningsgrunnlagPrStatus(Sammenligningsgrunnlag gammeltSG, SammenligningsgrunnlagType type) {
        this.avvikPromille = gammeltSG.getAvvikPromille();
        this.rapportertPrÅr = gammeltSG.getRapportertPrÅr();
        this.sammenligningsgrunnlagType = type;
        this.sammenligningsperiode = gammeltSG.getSammenligningsperiode();
    }

    private SammenligningsgrunnlagPrStatus() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSammenligningsperiodeFom() {
        return sammenligningsperiode.getFomDato();
    }

    public LocalDate getSammenligningsperiodeTom() {
        return sammenligningsperiode.getTomDato();
    }

    public BigDecimal getRapportertPrÅr() {
        return rapportertPrÅr;
    }

    public BigDecimal getAvvikPromille() {
        return avvikPromille;
    }

    public SammenligningsgrunnlagType getSammenligningsgrunnlagType() {
        return sammenligningsgrunnlagType;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SammenligningsgrunnlagPrStatus)) {
            return false;
        }
        var other = (SammenligningsgrunnlagPrStatus) obj;
        return Objects.equals(this.getBeregningsgrunnlag(), other.getBeregningsgrunnlag())
                && Objects.equals(this.getSammenligningsgrunnlagType(), other.getSammenligningsgrunnlagType())
                && Objects.equals(this.getSammenligningsperiodeFom(), other.getSammenligningsperiodeFom())
                && Objects.equals(this.getSammenligningsperiodeTom(), other.getSammenligningsperiodeTom())
                && Objects.equals(this.getRapportertPrÅr(), other.getRapportertPrÅr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlag, sammenligningsgrunnlagType, sammenligningsperiode, rapportertPrÅr, avvikPromille);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "sammenligningsgrunnlagType=" + sammenligningsgrunnlagType + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeFom=" + sammenligningsperiode.getFomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeTom=" + sammenligningsperiode.getTomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "rapportertPrÅr=" + rapportertPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avvikPromille=" + avvikPromille + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }

    private static SammenligningsgrunnlagType finnSGType(List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        if (aktivitetStatuser.stream().anyMatch(st -> st.getAktivitetStatus().erSelvstendigNæringsdrivende())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_SN;
        } else if (aktivitetStatuser.stream()
            .anyMatch(st -> st.getAktivitetStatus().erFrilanser() || st.getAktivitetStatus().erArbeidstaker())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
        }
        throw new IllegalStateException("Klarte ikke utlede sammenligningstype for gammelt grunnlag. Aktivitetstatuser var " + aktivitetStatuser);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SammenligningsgrunnlagPrStatus byggFraGammel(Sammenligningsgrunnlag gammeltSG,
                                           List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        var sgType = finnSGType(aktivitetStatuser);
        return new SammenligningsgrunnlagPrStatus(gammeltSG, sgType);
    }

    public static class Builder {
        private SammenligningsgrunnlagPrStatus sammenligningsgrunnlagMal;

        public Builder() {
            sammenligningsgrunnlagMal = new SammenligningsgrunnlagPrStatus();
        }

        public Builder medSammenligningsgrunnlagType(SammenligningsgrunnlagType sammenligningsgrunnlagType) {
            sammenligningsgrunnlagMal.sammenligningsgrunnlagType = sammenligningsgrunnlagType;
            return this;
        }

        public Builder medSammenligningsperiode(LocalDate fom, LocalDate tom) {
            sammenligningsgrunnlagMal.sammenligningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder medRapportertPrÅr(BigDecimal rapportertPrÅr) {
            sammenligningsgrunnlagMal.rapportertPrÅr = rapportertPrÅr;
            return this;
        }

        public Builder medAvvikPromille(BigDecimal avvikPromille) {
            if(avvikPromille != null) {
                sammenligningsgrunnlagMal.avvikPromille = avvikPromille;
            }
            return this;
        }

        SammenligningsgrunnlagPrStatus build(BeregningsgrunnlagEntitet kladd) {
            verifyStateForBuild();
            kladd.leggTilSammenligningsgrunnlagPrStatus(sammenligningsgrunnlagMal);
            return sammenligningsgrunnlagMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsgrunnlagType, "sammenligningsgrunnlagType");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode, "sammenligningsperiodePeriode");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getFomDato(), "sammenligningsperiodeFom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getTomDato(), "sammenligningsperiodeTom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.rapportertPrÅr, "rapportertPrÅr");
            Objects.requireNonNull(sammenligningsgrunnlagMal.avvikPromille, "avvikPromille");
        }
    }

}
