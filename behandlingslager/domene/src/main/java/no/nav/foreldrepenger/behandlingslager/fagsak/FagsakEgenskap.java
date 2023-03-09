package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "FagsakEgenskap")
@Table(name = "FAGSAK_EGENSKAP")
public class FagsakEgenskap extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_EGENSKAP")
    private Long id;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Convert(converter = EgenskapNøkkel.KodeverdiConverter.class)
    @Column(name="egenskap_key", nullable = false)
    private EgenskapNøkkel egenskapNøkkel;

    @Column(name = "egenskap_value")
    private String egenskapVerdi;

    public FagsakEgenskap() {
        // For Hibernate
    }

    FagsakEgenskap(Long fagsakId, EgenskapNøkkel nøkkel, String verdi) {
        this.fagsakId = fagsakId;
        this.egenskapNøkkel = nøkkel;
        this.egenskapVerdi = verdi;
    }

    FagsakEgenskap(FagsakEgenskap egenskap) {
        this.fagsakId = egenskap.fagsakId;
        this.egenskapNøkkel = egenskap.egenskapNøkkel;
        this.egenskapVerdi = egenskap.egenskapVerdi;
    }

    public Long getId() {
        return id;
    }

    void setAktiv(boolean aktivt) {
        this.aktiv = aktivt;
    }

    public boolean getErAktivt() {
        return aktiv;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public EgenskapNøkkel getEgenskapNøkkel() {
        return egenskapNøkkel;
    }

    public String getEgenskapVerdi() {
        return egenskapVerdi;
    }

    public <E extends Enum<E>> Optional<E> getEgenskapVerdiHvisFinnes(Class<E> enumCls) {
        return Optional.ofNullable(egenskapVerdi).map(v -> Enum.valueOf(enumCls, v));
    }

    public <E extends Enum<E>> E getEgenskapVerdi(Class<E> enumCls) {
        return egenskapVerdi != null ? Enum.valueOf(enumCls, egenskapVerdi) : null;
    }

    void fjernEgenskapVerdi() {
        this.egenskapVerdi = null;
    }

    void setEgenskapVerdi(String egenskapVerdi) {
        this.egenskapVerdi = egenskapVerdi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FagsakEgenskap that))
            return false;
        return fagsakId.equals(that.fagsakId) && egenskapNøkkel == that.egenskapNøkkel && Objects.equals(egenskapVerdi, that.egenskapVerdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, egenskapNøkkel);
    }
}
