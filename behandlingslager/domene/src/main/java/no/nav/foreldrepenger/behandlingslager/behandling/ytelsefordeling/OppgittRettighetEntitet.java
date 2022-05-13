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
    @Column(name = "mor_stonad_eos")
    @ChangeTracked
    private Boolean morMottarStønadEØS;


    OppgittRettighetEntitet() {
    }

    public OppgittRettighetEntitet(Boolean harAnnenForeldreRett,
                                   Boolean harAleneomsorgForBarnet,
                                   Boolean morMottarUføretrygd,
                                   Boolean morMottarStønadEØS) {
        this.harAnnenForeldreRett = harAnnenForeldreRett;
        this.harAleneomsorgForBarnet = harAleneomsorgForBarnet;
        this.morMottarUføretrygd = morMottarUføretrygd;
        this.morMottarStønadEØS = morMottarStønadEØS;
    }

    public Boolean getHarAleneomsorgForBarnet() {
        return harAleneomsorgForBarnet;
    }

    public Boolean getHarAnnenForeldreRett() {
        return harAnnenForeldreRett;
    }

    public boolean getMorMottarUføretrygd() {
        return morMottarUføretrygd != null && morMottarUføretrygd;
    }

    public boolean getMorMottarStønadEØS() {
        return morMottarStønadEØS != null && morMottarStønadEØS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OppgittRettighetEntitet) o;
        return Objects.equals(harAnnenForeldreRett, that.harAnnenForeldreRett) &&
            Objects.equals(harAleneomsorgForBarnet, that.harAleneomsorgForBarnet) &&
            Objects.equals(morMottarStønadEØS, that.morMottarStønadEØS) &&
            Objects.equals(morMottarUføretrygd, that.morMottarUføretrygd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(harAnnenForeldreRett, harAleneomsorgForBarnet, morMottarUføretrygd, morMottarStønadEØS);
    }

    @Override
    public String toString() {
        return "OppgittRettighetEntitet{" +
            "id=" + id +
            ", harAnnenForeldreRett=" + harAnnenForeldreRett +
            ", harAleneomsorgForBarnet=" + harAleneomsorgForBarnet +
            ", morUføretrygd=" + morMottarUføretrygd +
            ", morMottarStønadEØS=" + morMottarStønadEØS +
            '}';
    }
}
