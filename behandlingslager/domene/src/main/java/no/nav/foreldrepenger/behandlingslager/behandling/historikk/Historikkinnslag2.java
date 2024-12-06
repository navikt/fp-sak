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

    public static final String BOLD_MARKØR = "__";

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
    private List<Historikkinnslag2Linje> linjer = new ArrayList<>();

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

    public List<Historikkinnslag2Linje> getLinjer() {
        return linjer;
    }

    public List<Historikkinnslag2DokumentLink> getDokumentLinker() {
        return dokumentLinker;
    }

    @Override
    public String toString() {
        return "Historikkinnslag2{" + "fagsakId=" + fagsakId + ", behandlingId=" + behandlingId + ", aktør=" + aktør + ", skjermlenkeType="
            + skjermlenke + ", linjer=" + linjer + ", tittel='" + tittel + '\'' + '}';
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
            && Objects.equals(dokumentLinker, that.dokumentLinker) && Objects.equals(linjer, that.linjer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fagsakId, tittel, dokumentLinker, linjer);
    }

    public String getTittel() {
        return tittel;
    }

    public static class Builder {

        private Historikkinnslag2 kladd = new Historikkinnslag2();
        private List<HistorikkinnslagLinjeBuilder> internLinjer = new ArrayList<>();

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


        public Builder medLinjer(List<HistorikkinnslagLinjeBuilder> linjer) {
            internLinjer = new ArrayList<>(linjer.stream().filter(Objects::nonNull).toList()); // fraTilEquals kan legger til null objekter
            return this;
        }

        public Builder addLinje(HistorikkinnslagLinjeBuilder historikkinnslagLinjeBuilder) {
            if (historikkinnslagLinjeBuilder != null) {
                internLinjer.add(historikkinnslagLinjeBuilder);
            }
            return this;
        }

        public Builder addLinje(String tekst) {
            if (tekst != null) {
                internLinjer.add(new HistorikkinnslagLinjeBuilder().tekst(tekst));
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
            Objects.requireNonNull(kladd.fagsakId);
            Objects.requireNonNull(kladd.aktør);
            if (kladd.tittel == null && kladd.skjermlenke == null) {
                throw new NullPointerException("Forventer å enten ha tittel eller skjermlenke");
            }
            fjernLeadingOgTrailingLinjeskift();

            for (var i = 0; i < internLinjer.size(); i++) {
                var type = internLinjer.get(i).getType();
                var linje = type == HistorikkinnslagLinjeType.TEKST ? Historikkinnslag2Linje.tekst(sluttMedPunktum(internLinjer.get(i).tilTekst()),
                    i) : Historikkinnslag2Linje.linjeskift(i);
                kladd.linjer.add(linje);
                linje.setHistorikkinnslag(kladd);
            }

            var t = kladd;
            kladd = null;
            return t;
        }

        private void fjernLeadingOgTrailingLinjeskift() {
            if (!internLinjer.isEmpty()) {
                if (internLinjer.getFirst().getType() == HistorikkinnslagLinjeType.LINJESKIFT) {
                    internLinjer.removeFirst();
                }
                if (internLinjer.getLast().getType() == HistorikkinnslagLinjeType.LINJESKIFT) {
                    internLinjer.removeLast();
                }
            }
        }

        private String sluttMedPunktum(String tekst) {
            if (tekst.isEmpty()) {
                return tekst;
            }
            var sisteTegn = finnSisteTegn(tekst);
            return List.of(':', '.', '?', '!').contains(sisteTegn) ? tekst : tekst + ".";
        }

        private static char finnSisteTegn(String tekst) {
            if (tekst.endsWith(BOLD_MARKØR)) {
                return tekst.charAt(tekst.length() - 1 - BOLD_MARKØR.length());
            }
            return tekst.charAt(tekst.length() - 1);
        }
    }
}
