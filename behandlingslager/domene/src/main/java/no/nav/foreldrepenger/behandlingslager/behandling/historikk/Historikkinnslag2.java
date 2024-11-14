package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

@Entity(name = "Historikkinnslag2")
@Table(name = "HISTORIKKINNSLAG2")
public class Historikkinnslag2 extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG2")
    private Long id;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    @Column(name = "behandling_id")
    private Long behandlingId;

    @Convert(converter = HistorikkAktør.KodeverdiConverter.class)
    @Column(name="aktoer", nullable = false)
    private HistorikkAktør aktør;

    @Convert(converter = SkjermlenkeType.KodeverdiConverter.class)
    @Column(name="skjermlenke")
    private SkjermlenkeType skjermlenke;

    @OneToMany(mappedBy = "historikkinnslag")
    private List<Historikkinnslag2Tekstlinje> tekstlinjer = new ArrayList<>();

    @Column(name = "tittel")
    private String tittel;

    public Historikkinnslag2(Long fagsakId,
                             Long behandlingId,
                             HistorikkAktør aktør,
                             SkjermlenkeType skjermlenke,
                             String tittel,
                             List<Historikkinnslag2Tekstlinje> tekstlinjer) {
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.aktør = aktør;
        this.skjermlenke = skjermlenke;
        this.tittel = tittel;
        this.tekstlinjer = tekstlinjer;
        tekstlinjer.forEach(t -> t.setHistorikkinnslag(this));
    }

    protected Historikkinnslag2() {
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public HistorikkAktør getAktør() {
        return aktør;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public List<Historikkinnslag2Tekstlinje> getTekstlinjer() {
        return tekstlinjer;
    }

    @Override
    public String toString() {
        return "Historikkinnslag2{" + "fagsakId=" + fagsakId + ", behandlingId=" + behandlingId + ", aktør=" + aktør + ", skjermlenkeType="
            + skjermlenke + ", tekstlinjer=" + tekstlinjer + ", tittel='" + tittel + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag2 that)) {
            return false;
        }
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(fagsakId, that.fagsakId) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(tekstlinjer, that.tekstlinjer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fagsakId, tittel, tekstlinjer);
    }

    public String getTittel() {
        return tittel;
    }
}
