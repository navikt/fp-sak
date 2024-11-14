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
    @Column(name = "aktoer", nullable = false)
    private HistorikkAktør aktør;

    @Convert(converter = SkjermlenkeType.KodeverdiConverter.class)
    @Column(name = "skjermlenke")
    private SkjermlenkeType skjermlenke;

    @OneToMany(mappedBy = "historikkinnslag")
    private List<Historikkinnslag2Tekstlinje> tekstlinjer = new ArrayList<>();

    @OneToMany(mappedBy = "historikkinnslag")
    private List<Historikkinnslag2DokumentLink> dokumentLinker = new ArrayList<>();

    @Column(name = "tittel")
    private String tittel;

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

    public List<Historikkinnslag2DokumentLink> getDokumentLinker() {
        return dokumentLinker;
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
        return Objects.equals(behandlingId, that.behandlingId) && Objects.equals(fagsakId, that.fagsakId) && Objects.equals(tittel, that.tittel)
            && Objects.equals(dokumentLinker, that.dokumentLinker) && Objects.equals(tekstlinjer, that.tekstlinjer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fagsakId, tittel, dokumentLinker, tekstlinjer);
    }

    public String getTittel() {
        return tittel;
    }

    public static class Builder {

        private Historikkinnslag2 kladd = new Historikkinnslag2();
        private List<String> internLinjer = new ArrayList<>();

        public Builder medFagsakId(Long fagsakId) {
            kladd.fagsakId = fagsakId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medAktør(HistorikkAktør aktør) {
            kladd.aktør = aktør;
            return this;
        }

        public Builder medTittel(SkjermlenkeType skjermlenke) {
            kladd.skjermlenke = skjermlenke;
            return this;
        }

        public Builder medTittel(String tittel) {
            kladd.tittel = tittel;
            return this;
        }


        public Builder medTekstlinjer(List<HistorikkinnslagTekstlinjeBuilder> linjer) {
            internLinjer = linjer.stream().map(HistorikkinnslagTekstlinjeBuilder::build).toList();
            return this;
        }

        public Builder addTekstlinje(HistorikkinnslagTekstlinjeBuilder historikkinnslagTekstlinjeBuilder) {
            if (historikkinnslagTekstlinjeBuilder != null) {
                internLinjer.add(historikkinnslagTekstlinjeBuilder.build());
            }
            return this;
        }

        public Builder addTekstlinje(String tekst) {
            if (tekst != null) {
                internLinjer.add(tekst);
            }
            return this;
        }

        public Builder medDokumenter(List<Historikkinnslag2DokumentLink> dokumenter) {
            kladd.dokumentLinker = dokumenter;
            for (var historikkinnslag2DokumentLink : dokumenter) {
                historikkinnslag2DokumentLink.setHistorikkinnslag(kladd);
            }
            return this;
        }

        public Historikkinnslag2 build() {
            for (int i = 0; i < internLinjer.size(); i++) {
                var linje = new Historikkinnslag2Tekstlinje(internLinjer.get(i), String.valueOf(i));
                kladd.tekstlinjer.add(linje);
                linje.setHistorikkinnslag(kladd);
            }

            var t = kladd;
            kladd = null;
            return t;
        }

        public int antallLagtTilLinjer() {
            return kladd.tekstlinjer.size();
        }
    }
}
