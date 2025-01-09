package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@Entity(name = "HistorikkinnslagDel")
@Table(name = "HISTORIKKINNSLAG_DEL")
public class HistorikkinnslagDel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG_DEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false, updatable = false)
    @JsonBackReference
    private HistorikkinnslagOld historikkinnslag;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "historikkinnslagDel")
    private List<HistorikkinnslagFelt> historikkinnslagFelt = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public HistorikkinnslagOld getHistorikkinnslag() {
        return historikkinnslag;
    }

    public List<HistorikkinnslagFelt> getHistorikkinnslagFelt() {
        return historikkinnslagFelt;
    }

    public Optional<HistorikkinnslagFelt> getAarsakFelt() {
        return finnFelt(HistorikkinnslagFeltType.AARSAK);
    }

    public Optional<HistorikkinnslagFelt> getTema() {
        return finnFelt(HistorikkinnslagFeltType.ANGÅR_TEMA);
    }


    public Optional<HistorikkinnslagFelt> getAvklartSoeknadsperiode() {
        return finnFelt(HistorikkinnslagFeltType.AVKLART_SOEKNADSPERIODE);
    }

    public Optional<String> getBegrunnelse() {
        return finnFeltTilVerdi(HistorikkinnslagFeltType.BEGRUNNELSE);
    }

    public Optional<HistorikkinnslagFelt> getBegrunnelseFelt() {
        return finnFelt(HistorikkinnslagFeltType.BEGRUNNELSE);
    }

    /**
     * Hent en hendelse
     * @return Et HistorikkinnslagFelt fordi vi trenger navn (f.eks. BEH_VENT) og tilVerdi (f.eks. <fristDato>)
     */
    public Optional<HistorikkinnslagFelt> getHendelse() {
        return finnFelt(HistorikkinnslagFeltType.HENDELSE);
    }

    public Optional<String> getResultat() {
        return finnFeltTilVerdi(HistorikkinnslagFeltType.RESULTAT);
    }

    public Optional<HistorikkinnslagFelt> getResultatFelt() {
        return finnFelt(HistorikkinnslagFeltType.RESULTAT);
    }

    public Optional<HistorikkinnslagFelt> getGjeldendeFraFelt() {
        return finnFelt(HistorikkinnslagFeltType.GJELDENDE_FRA);
    }

    public Optional<String> getSkjermlenke() {
        return finnFeltTilVerdi(HistorikkinnslagFeltType.SKJERMLENKE);
    }

    public List<HistorikkinnslagFelt> getEndredeFelt() {
        return finnFeltListe(HistorikkinnslagFeltType.ENDRET_FELT);
    }

    public List<HistorikkinnslagFelt> getOpplysninger() {
        return finnFeltListe(HistorikkinnslagFeltType.OPPLYSNINGER);
    }

    public List<HistorikkinnslagTotrinnsvurdering> getTotrinnsvurderinger() {
        var aksjonspunktFeltTypeKoder = Arrays.asList(
            HistorikkinnslagFeltType.AKSJONSPUNKT_BEGRUNNELSE,
            HistorikkinnslagFeltType.AKSJONSPUNKT_GODKJENT,
            HistorikkinnslagFeltType.AKSJONSPUNKT_KODE
        );

        var alleAksjonspunktFelt = historikkinnslagFelt.stream()
            .filter(felt -> aksjonspunktFeltTypeKoder.contains(felt.getFeltType()))
            .toList();

        return alleAksjonspunktFelt.stream()
            .collect(Collectors.groupingBy(HistorikkinnslagFelt::getSekvensNr))
            .entrySet()
            .stream()
            .map(entry -> lagHistorikkinnslagAksjonspunkt(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getSekvensNr))
            .toList();
    }

    private HistorikkinnslagTotrinnsvurdering lagHistorikkinnslagAksjonspunkt(Integer sekvensNr, List<HistorikkinnslagFelt> aksjonspunktFeltListe) {
        var aksjonspunkt = new HistorikkinnslagTotrinnsvurdering(sekvensNr);
        aksjonspunktFeltListe.forEach(felt -> {
            if (HistorikkinnslagFeltType.AKSJONSPUNKT_BEGRUNNELSE.equals(felt.getFeltType())) {
                aksjonspunkt.setBegrunnelse(felt.getTilVerdi());
            } else if (HistorikkinnslagFeltType.AKSJONSPUNKT_GODKJENT.equals(felt.getFeltType())) {
                aksjonspunkt.setGodkjent(Boolean.parseBoolean(felt.getTilVerdi()));
            } else if (HistorikkinnslagFeltType.AKSJONSPUNKT_KODE.equals(felt.getFeltType())) {
                var aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(felt.getTilVerdi());
                aksjonspunkt.setAksjonspunktDefinisjon(aksjonspunktDefinisjon);
            } else {
                throw new IllegalStateException("Uventet feltnavn " + felt.getFeltType().getKode());
            }
        });
        return aksjonspunkt;
    }

    private Optional<HistorikkinnslagFelt> finnFelt(HistorikkinnslagFeltType historikkinnslagFeltType) {
        return historikkinnslagFelt.stream()
            .filter(felt -> historikkinnslagFeltType.equals(felt.getFeltType()))
            .findFirst();
    }

    private Optional<String> finnFeltTilVerdi(HistorikkinnslagFeltType historikkinnslagFeltType) {
        return finnFelt(historikkinnslagFeltType)
            .map(HistorikkinnslagFelt::getTilVerdi);
    }

    private List<HistorikkinnslagFelt> finnFeltListe(HistorikkinnslagFeltType feltType) {
        return historikkinnslagFelt.stream()
            .filter(felt -> felt.getFeltType().equals(feltType))
            .sorted(Comparator.comparing(HistorikkinnslagFelt::getSekvensNr))
            .toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HistorikkinnslagDel.Builder builder(HistorikkinnslagDel del) {
        return new Builder(del);
    }

    public static class Builder {
        private HistorikkinnslagDel kladd;


        private Builder() {
            this(new HistorikkinnslagDel());
        }

        public Builder(HistorikkinnslagDel del) {
            kladd = del;
        }

        public Builder leggTilFelt(HistorikkinnslagFelt felt) {
            kladd.historikkinnslagFelt.add(felt);
            felt.setHistorikkinnslagDel(kladd);
            return this;
        }

        public Builder medHistorikkinnslag(HistorikkinnslagOld historikkinnslag) {
            kladd.historikkinnslag = historikkinnslag;
            return this;
        }

        public boolean harFelt() {
            return !kladd.getHistorikkinnslagFelt().isEmpty();
        }

        public HistorikkinnslagDel build() {
            return kladd;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorikkinnslagDel that)) {
            return false;
        }
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
