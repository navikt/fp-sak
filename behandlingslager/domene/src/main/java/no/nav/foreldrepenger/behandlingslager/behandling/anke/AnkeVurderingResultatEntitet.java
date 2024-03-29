package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Convert(converter = AnkeVurdering.KodeverdiConverter.class)
    @Column(name="tr_vurdering", nullable = false)
    private AnkeVurdering trygderettVurdering = AnkeVurdering.UDEFINERT;

    @Convert(converter = AnkeOmgjørÅrsak.KodeverdiConverter.class)
    @Column(name="tr_omgjoer_aarsak", nullable = false)
    private AnkeOmgjørÅrsak trygderettOmgjørÅrsak = AnkeOmgjørÅrsak.UDEFINERT;

    @Convert(converter = AnkeVurderingOmgjør.KodeverdiConverter.class)
    @Column(name="tr_vurdering_omgjoer", nullable = false)
    private AnkeVurderingOmgjør trygderettVurderingOmgjør = AnkeVurderingOmgjør.UDEFINERT;

    @Column(name = "sendt_trygderett_dato")
    private LocalDate sendtTrygderettDato;

    public AnkeVurderingResultatEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AnkeVurderingResultatEntitet ankeVurderingResultatEntitet) {
        return new Builder(ankeVurderingResultatEntitet);
    }

    private AnkeVurderingResultatEntitet(AnkeVurderingResultatEntitet entitet) {
        this.ankeResultat = entitet.ankeResultat;
        this.ankeVurdering = entitet.ankeVurdering;
        this.ankeOmgjørÅrsak = entitet.ankeOmgjørÅrsak;
        this.ankeVurderingOmgjør = entitet.ankeVurderingOmgjør;
        this.begrunnelse = entitet.begrunnelse;
        this.fritekstTilBrev = entitet.fritekstTilBrev;
        this.merknaderFraBruker = entitet.merknaderFraBruker;
        this.erMerknaderMottatt = entitet.erMerknaderMottatt;
        this.godkjentAvMedunderskriver = entitet.godkjentAvMedunderskriver;
        this.gjelderVedtak = entitet.gjelderVedtak;
        this.erAnkerIkkePart = entitet.erAnkerIkkePart;
        this.erFristIkkeOverholdt = entitet.erFristIkkeOverholdt;
        this.erIkkeKonkret = entitet.erIkkeKonkret;
        this.erIkkeSignert = entitet.erIkkeSignert;
        this.erSubsidiartRealitetsbehandles = entitet.erSubsidiartRealitetsbehandles;
        this.trygderettVurdering = entitet.trygderettVurdering;
        this.trygderettVurderingOmgjør = entitet.trygderettVurderingOmgjør;
        this.trygderettOmgjørÅrsak = entitet.trygderettOmgjørÅrsak;
        this.sendtTrygderettDato = entitet.sendtTrygderettDato;
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

    public AnkeVurdering getTrygderettVurdering() {
        return trygderettVurdering;
    }

    public AnkeOmgjørÅrsak getTrygderettOmgjørÅrsak() {
        return trygderettOmgjørÅrsak;
    }

    public AnkeVurderingOmgjør getTrygderettVurderingOmgjør() {
        return trygderettVurderingOmgjør;
    }

    public LocalDate getSendtTrygderettDato() {
        return sendtTrygderettDato;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (AnkeVurderingResultatEntitet) o;
        return harLikVurdering(that) &&
            erMerknaderMottatt == that.erMerknaderMottatt &&
            gjelderVedtak == that.gjelderVedtak &&
            godkjentAvMedunderskriver == that.godkjentAvMedunderskriver &&
            Objects.equals(ankeResultat, that.ankeResultat) &&
            Objects.equals(fritekstTilBrev, that.fritekstTilBrev) &&
            Objects.equals(merknaderFraBruker, that.merknaderFraBruker);
    }

    public boolean harLikVurdering(AnkeVurderingResultatEntitet that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return erAnkerIkkePart == that.erAnkerIkkePart &&
            erFristIkkeOverholdt == that.erFristIkkeOverholdt &&
            erIkkeKonkret == that.erIkkeKonkret &&
            erIkkeSignert == that.erIkkeSignert &&
            erSubsidiartRealitetsbehandles == that.erSubsidiartRealitetsbehandles &&
            ankeVurdering == that.ankeVurdering &&
            ankeOmgjørÅrsak == that.ankeOmgjørÅrsak &&
            ankeVurderingOmgjør == that.ankeVurderingOmgjør &&
            trygderettVurdering == that.trygderettVurdering &&
            trygderettOmgjørÅrsak == that.trygderettOmgjørÅrsak &&
            trygderettVurderingOmgjør == that.trygderettVurderingOmgjør &&
            Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ankeResultat, ankeVurdering, ankeOmgjørÅrsak, ankeVurderingOmgjør, begrunnelse, fritekstTilBrev,
            merknaderFraBruker, erMerknaderMottatt, gjelderVedtak, erAnkerIkkePart, erFristIkkeOverholdt, erIkkeKonkret, erIkkeSignert, erSubsidiartRealitetsbehandles,
            trygderettVurdering, trygderettVurderingOmgjør, trygderettOmgjørÅrsak, sendtTrygderettDato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "ankeVurdering=" + getAnkeVurdering() + ", "
            + "ankeVurderingOmgjør" + getAnkeVurderingOmgjør() + ", "
            + "ankeOmgjørÅrsak=" + getAnkeOmgjørÅrsak() + ", "
            + "begrunnelse=" + begrunnelse + ", "
            + "fritekstTilBrev=" + fritekstTilBrev + ", "
            + ">";
    }

    public static class Builder {
        private AnkeVurderingResultatEntitet ankeVurderingResultatMal;

        private Builder() {
            ankeVurderingResultatMal = new AnkeVurderingResultatEntitet();
        }

        private Builder(AnkeVurderingResultatEntitet ankeVurderingResultatEntitet) {
            ankeVurderingResultatMal = new AnkeVurderingResultatEntitet(ankeVurderingResultatEntitet);
        }

        public Builder medAnkeVurdering(AnkeVurdering ankeVurdering) {
            ankeVurderingResultatMal.ankeVurdering = ankeVurdering == null ? AnkeVurdering.UDEFINERT : ankeVurdering;
            return this;
        }

        public Builder medAnkeVurderingOmgjør(AnkeVurderingOmgjør ankeVurderingOmgjør) {
            ankeVurderingResultatMal.ankeVurderingOmgjør = ankeVurderingOmgjør == null ? AnkeVurderingOmgjør.UDEFINERT : ankeVurderingOmgjør;
            return this;
        }

        public Builder medAnkeResultat(AnkeResultatEntitet ankeResultat) {
            ankeVurderingResultatMal.ankeResultat = ankeResultat;
            return this;
        }

        public Builder medSendtTrygderettDato(LocalDate sendtTrygderettDato) {
            ankeVurderingResultatMal.sendtTrygderettDato = sendtTrygderettDato;
            return this;
        }

        public Builder medTrygderettVurdering(AnkeVurdering trVurdering) {
            ankeVurderingResultatMal.trygderettVurdering = trVurdering == null ? AnkeVurdering.UDEFINERT : trVurdering;
            return this;
        }

        public Builder medTrygderettVurderingOmgjør(AnkeVurderingOmgjør trVurderingOmgjør) {
            ankeVurderingResultatMal.trygderettVurderingOmgjør = trVurderingOmgjør == null ? AnkeVurderingOmgjør.UDEFINERT : trVurderingOmgjør;
            return this;
        }

        public AnkeVurderingResultatEntitet build() {
            verifyStateForBuild();
            return ankeVurderingResultatMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(ankeVurderingResultatMal.ankeResultat, "AnkeResultat");
            if (ankeVurderingResultatMal.ankeVurdering.equals(AnkeVurdering.ANKE_OMGJOER)) {
                Objects.requireNonNull(ankeVurderingResultatMal.ankeVurderingOmgjør, "ankeVurderingOmgjør");
            }
        }
    }
}
