package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "AnkeVurderingResultat")
@Table(name = "ANKE_VURDERING_RESULTAT")
public class AnkeVurderingResultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANKE_VURDERING_RESULTAT")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anke_resultat_id", nullable = false, updatable = false)
    private AnkeResultatEntitet ankeResultat;

    @Convert(converter = AnkeVurdering.KodeverdiConverter.class)
    @Column(name="ankevurdering", nullable = false)
    private AnkeVurdering ankeVurdering = AnkeVurdering.UDEFINERT;

    @Convert(converter = AnkeOmgjørÅrsak.KodeverdiConverter.class)
    @Column(name="anke_omgjoer_aarsak", nullable = false)
    private AnkeOmgjørÅrsak ankeOmgjørÅrsak = AnkeOmgjørÅrsak.UDEFINERT;

    @Convert(converter = AnkeVurderingOmgjør.KodeverdiConverter.class)
    @Column(name="anke_vurdering_omgjoer", nullable = false)
    private AnkeVurderingOmgjør ankeVurderingOmgjør = AnkeVurderingOmgjør.UDEFINERT;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "fritekst_til_brev")
    private String fritekstTilBrev;

    @Column(name = "merknader_fra_bruker")
    private String merknaderFraBruker;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_merknader_mottatt", nullable = false)
    private boolean erMerknaderMottatt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "godkjent_av_medunderskriver", nullable = false)
    private boolean godkjentAvMedunderskriver;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "gjelder_vedtak", nullable = false)
    private boolean gjelderVedtak;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_anker_ikke_part", nullable = false)
    private boolean erAnkerIkkePart;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_frist_ikke_overholdt", nullable = false)
    private boolean erFristIkkeOverholdt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_ikke_konkret", nullable = false)
    private boolean erIkkeKonkret;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_ikke_signert", nullable = false)
    private boolean erIkkeSignert;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_subsidiart_realitet_beh", nullable = false)
    private boolean erSubsidiartRealitetsbehandles;

    public AnkeVurderingResultatEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public AnkeOmgjørÅrsak getAnkeOmgjørÅrsak() {
        return ankeOmgjørÅrsak;
    }

    public AnkeVurderingOmgjør getAnkeVurderingOmgjør() {
        return ankeVurderingOmgjør;
    }

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String getMerknaderFraBruker() {
        return merknaderFraBruker;
    }

    public boolean getErMerknaderMottatt() {
        return erMerknaderMottatt;
    }

    public AnkeResultatEntitet getAnkeResultat() {
        return ankeResultat;
    }

    public boolean getGjelderVedtak() {
        return gjelderVedtak;
    }

    public boolean godkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public boolean erAnkerIkkePart() {
        return erAnkerIkkePart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public List<AnkeAvvistÅrsak> hentAvvistÅrsaker(){
        List<AnkeAvvistÅrsak> avvistÅrsaker = new ArrayList<>();
        if(erFristIkkeOverholdt()){
            avvistÅrsaker.add(AnkeAvvistÅrsak.ANKE_FOR_SENT);
        }
        if(erAnkerIkkePart()){
            avvistÅrsaker.add(AnkeAvvistÅrsak.ANKE_IKKE_PART);
        }
        if(erIkkeKonkret()){
            avvistÅrsaker.add(AnkeAvvistÅrsak.ANKE_IKKE_KONKRET);
        }
        if(erIkkeSignert()){
            avvistÅrsaker.add(AnkeAvvistÅrsak.ANKE_IKKE_SIGNERT);
        }
        if(!gjelderVedtak){
            avvistÅrsaker.add(AnkeAvvistÅrsak.ANKE_UGYLDIG);
        }
        return avvistÅrsaker;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof AnkeVurderingResultatEntitet)) {
            return false;
        }
        AnkeVurderingResultatEntitet other = (AnkeVurderingResultatEntitet) obj;
        return Objects.equals(this.ankeVurdering, other.getAnkeVurdering())
            && Objects.equals(this.getAnkeOmgjørÅrsak(), other.getAnkeOmgjørÅrsak())
            && Objects.equals(this.begrunnelse, other.begrunnelse)
            && Objects.equals(this.fritekstTilBrev, other.fritekstTilBrev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAnkeVurdering(), getAnkeOmgjørÅrsak(), begrunnelse, fritekstTilBrev);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "ankeVurdering=" + getAnkeVurdering() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "ankeVurderingOmgjør" + getAnkeVurderingOmgjør() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "ankeOmgjørÅrsak=" + getAnkeOmgjørÅrsak() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "begrunnelse=" + begrunnelse + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "fritekstTilBrev=" + fritekstTilBrev + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private AnkeVurderingResultatEntitet ankeVurderingResultatMal;

        public Builder() {
            ankeVurderingResultatMal = new AnkeVurderingResultatEntitet();
        }


        public Builder medAnkeVurdering(AnkeVurdering ankeVurdering) {
            ankeVurderingResultatMal.ankeVurdering = ankeVurdering == null ? AnkeVurdering.UDEFINERT : ankeVurdering;
            return this;
        }

        public Builder medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak ankeOmgjørÅrsak) {
            ankeVurderingResultatMal.ankeOmgjørÅrsak = ankeOmgjørÅrsak == null ? AnkeOmgjørÅrsak.UDEFINERT : ankeOmgjørÅrsak;
            return this;
        }

        public Builder medAnkeVurderingOmgjør(AnkeVurderingOmgjør ankeVurderingOmgjør) {
            ankeVurderingResultatMal.ankeVurderingOmgjør = ankeVurderingOmgjør == null ? AnkeVurderingOmgjør.UDEFINERT : ankeVurderingOmgjør;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            ankeVurderingResultatMal.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medFritekstTilBrev(String fritekstTilBrev) {
            ankeVurderingResultatMal.fritekstTilBrev = fritekstTilBrev;
            return this;
        }

        public Builder medMerknaderFraBruker(String merknaderFraBruker) {
            ankeVurderingResultatMal.merknaderFraBruker = merknaderFraBruker;
            return this;
        }

        public Builder medErMerknaderMottatt(boolean erMerknaderMottatt) {
            ankeVurderingResultatMal.erMerknaderMottatt = erMerknaderMottatt;
            return this;
        }

        public Builder medAnkeResultat(AnkeResultatEntitet ankeResultat) {
            ankeVurderingResultatMal.ankeResultat = ankeResultat;
            return this;
        }

        public Builder medGodkjentAvMedunderskriver(boolean godkjent) {
            ankeVurderingResultatMal.godkjentAvMedunderskriver = godkjent;
            return this;
        }

        public Builder medGjelderVedtak(boolean gjelderVedtak) {
            ankeVurderingResultatMal.gjelderVedtak = gjelderVedtak;
            return this;
        }

        public Builder medErAnkerIkkePart(boolean erAnkerIkkePart) {
            ankeVurderingResultatMal.erAnkerIkkePart = erAnkerIkkePart;
            return this;
        }

        public Builder medErFristIkkeOverholdt(boolean erFristIkkeOverholdt) {
            ankeVurderingResultatMal.erFristIkkeOverholdt = erFristIkkeOverholdt;
            return this;
        }

        public Builder medErIkkeKonkret(boolean erIkkeKonkret) {
            ankeVurderingResultatMal.erIkkeKonkret = erIkkeKonkret;
            return this;
        }

        public Builder medErIkkeSignert(boolean erIkkeSignert) {
            ankeVurderingResultatMal.erIkkeSignert = erIkkeSignert;
            return this;
        }

        public Builder medErSubsidiartRealitetsbehandles(boolean erSubsidiartRealitetsbehandles) {
            ankeVurderingResultatMal.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
            return this;
        }

        public AnkeVurderingResultatEntitet build() {
            verifyStateForBuild();
            return ankeVurderingResultatMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(ankeVurderingResultatMal.ankeResultat, "AnkeResultat");
            if (ankeVurderingResultatMal.ankeVurdering.equals(AnkeVurdering.ANKE_OMGJOER)) {
                Objects.requireNonNull(ankeVurderingResultatMal.ankeOmgjørÅrsak, "ankeOmgjørÅrsak");
                Objects.requireNonNull(ankeVurderingResultatMal.ankeVurderingOmgjør, "ankeVurderingOmgjør");
            }
        }
    }
}
