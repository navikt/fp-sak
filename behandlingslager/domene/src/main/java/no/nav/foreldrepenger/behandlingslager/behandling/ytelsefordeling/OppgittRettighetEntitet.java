package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "OppgittRettighet")
@Table(name = "SO_RETTIGHET")
public class OppgittRettighetEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SO_RETTIGHET")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "annen_foreldre_rett")
    @ChangeTracked
    private Boolean harAnnenForeldreRett;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aleneomsorg")
    @ChangeTracked
    private Boolean harAleneomsorgForBarnet;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "mor_uforetrygd")
    @ChangeTracked
    private Boolean morMottarUføretrygd;


    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "ANNEN_FORELDER_RETT_EOS")
    @ChangeTracked
    private Boolean annenForelderRettEØS;


    OppgittRettighetEntitet() {
    }

    public OppgittRettighetEntitet(Boolean harAnnenForeldreRett,
                                   Boolean harAleneomsorgForBarnet,
                                   Boolean morMottarUføretrygd,
                                   Boolean annenForelderRettEØS) {
        this.harAnnenForeldreRett = harAnnenForeldreRett;
        this.harAleneomsorgForBarnet = harAleneomsorgForBarnet;
        this.morMottarUføretrygd = morMottarUføretrygd;
        this.annenForelderRettEØS = annenForelderRettEØS;
    }

    public Boolean getHarAleneomsorgForBarnet() {
        return harAleneomsorgForBarnet;
    }

    public Boolean getHarAnnenForeldreRett() {
        return harAnnenForeldreRett;
    }

    public Boolean getMorMottarUføretrygd() {
        return morMottarUføretrygd;
    }

    public Boolean getAnnenForelderRettEØSNullable() {
        return annenForelderRettEØS;
    }

    public boolean getAnnenForelderRettEØS() {
        return annenForelderRettEØS != null && annenForelderRettEØS;
    }

    public static OppgittRettighetEntitet kopiAleneomsorg(OppgittRettighetEntitet rett, Boolean aleneomsorg) {
        if (rett == null) {
            return new OppgittRettighetEntitet(null, aleneomsorg, null, null);
        }
        return Objects.equals(aleneomsorg, rett.harAleneomsorgForBarnet) ? rett :
            new OppgittRettighetEntitet(rett.harAnnenForeldreRett, aleneomsorg, rett.morMottarUføretrygd, rett.annenForelderRettEØS);
    }

    public static OppgittRettighetEntitet kopiAleneomsorgIkkeRettAnnenForelder(OppgittRettighetEntitet rett) {
        if (rett == null) {
            return new OppgittRettighetEntitet(false, true, null, null);
        }
        return Objects.equals(rett.harAleneomsorgForBarnet, Boolean.TRUE) && Objects.equals(rett.harAnnenForeldreRett, Boolean.FALSE) ? rett :
            new OppgittRettighetEntitet(false, true, rett.morMottarUføretrygd, rett.annenForelderRettEØS);
    }

    public static OppgittRettighetEntitet kopiAnnenForelderRett(OppgittRettighetEntitet rett, Boolean annenForelderRett) {
        if (rett == null) {
            return new OppgittRettighetEntitet(annenForelderRett, null, null, null);
        }
        return Objects.equals(annenForelderRett, rett.harAnnenForeldreRett) ? rett :
            new OppgittRettighetEntitet(annenForelderRett, rett.harAleneomsorgForBarnet, rett.morMottarUføretrygd, rett.annenForelderRettEØS);
    }

    public static OppgittRettighetEntitet kopiAnnenForelderRettEØS(OppgittRettighetEntitet rett, Boolean annenForelderRettEØS) {
        if (rett == null) {
            return new OppgittRettighetEntitet(null, null, null, annenForelderRettEØS);
        }
        return Objects.equals(annenForelderRettEØS, rett.annenForelderRettEØS) ? rett :
            new OppgittRettighetEntitet(rett.harAnnenForeldreRett, rett.harAleneomsorgForBarnet, rett.morMottarUføretrygd, annenForelderRettEØS);
    }

    public static OppgittRettighetEntitet kopiMorUføretrygd(OppgittRettighetEntitet rett, Boolean morUføretrygd) {
        if (rett == null) {
            return new OppgittRettighetEntitet(null, null, morUføretrygd, null);
        }
        return Objects.equals(morUføretrygd, rett.morMottarUføretrygd) ? rett :
            new OppgittRettighetEntitet(rett.harAnnenForeldreRett, rett.harAleneomsorgForBarnet, morUføretrygd, rett.annenForelderRettEØS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OppgittRettighetEntitet) o;
        return Objects.equals(harAnnenForeldreRett, that.harAnnenForeldreRett) &&
            Objects.equals(harAleneomsorgForBarnet, that.harAleneomsorgForBarnet) &&
            Objects.equals(annenForelderRettEØS, that.annenForelderRettEØS) &&
            Objects.equals(morMottarUføretrygd, that.morMottarUføretrygd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(harAnnenForeldreRett, harAleneomsorgForBarnet, morMottarUføretrygd, annenForelderRettEØS);
    }

    @Override
    public String toString() {
        return "OppgittRettighetEntitet{" +
            "id=" + id +
            ", harAnnenForeldreRett=" + harAnnenForeldreRett +
            ", harAleneomsorgForBarnet=" + harAleneomsorgForBarnet +
            ", morUføretrygd=" + morMottarUføretrygd +
            ", annenForelderRettEØS=" + annenForelderRettEØS +
            '}';
    }
}
