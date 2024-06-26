package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;


@Entity(name = "BeregningsresultatFPAggregatEntitet")
@Table(name = "BR_RESULTAT_BEHANDLING")
public class BehandlingBeregningsresultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_RESULTAT_BEHANDLING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @OneToOne(optional = false)
    @JoinColumn(name = "bg_beregningsresultat_fp_id", nullable = false, updatable = false, unique = true)
    private BeregningsresultatEntitet bgBeregningsresultatFP;

    @OneToOne
    @JoinColumn(name = "utbet_beregningsresultat_fp_id", updatable = false, unique = true)
    private BeregningsresultatEntitet utbetBeregningsresultatFP;

    @OneToOne
    @JoinColumn(name = "beregningsresultat_feriepenger_id", updatable = false, unique = true)
    private BeregningsresultatFeriepenger beregningsresultatFeriepenger;

    @Column(name = "hindre_tilbaketrekk")
    @Convert(converter = BooleanToStringConverter.class)
    private Boolean skalHindreTilbaketrekk;

    BehandlingBeregningsresultatEntitet() {

    }

    public BehandlingBeregningsresultatEntitet(BehandlingBeregningsresultatEntitet kladd) {
        this.behandlingId = kladd.behandlingId;
        this.bgBeregningsresultatFP = kladd.bgBeregningsresultatFP;
        this.beregningsresultatFeriepenger = kladd.beregningsresultatFeriepenger;
        this.skalHindreTilbaketrekk = kladd.skalHindreTilbaketrekk;
    }

    public Long getId() {
        return id;
    }

    public BeregningsresultatEntitet getBgBeregningsresultatFP() {
        return bgBeregningsresultatFP;
    }

    public Optional<BeregningsresultatEntitet> getUtbetBeregningsresultatFP() {
        return Optional.ofNullable(utbetBeregningsresultatFP);
    }

    public Optional<BeregningsresultatFeriepenger> getBeregningsresultatFeriepenger() {
        return Optional.ofNullable(beregningsresultatFeriepenger);
    }

    public BeregningsresultatEntitet getGjeldendeBeregningsresultat() {
        return getUtbetBeregningsresultatFP().orElse(bgBeregningsresultatFP);
    }

    public List<BeregningsresultatPeriode> getGjeldendePerioder() {
        return getGjeldendeBeregningsresultat().getBeregningsresultatPerioder();
    }

    public List<BeregningsresultatFeriepengerPrÅr> getGjeldendeFeriepenger() {
        return getBeregningsresultatFeriepenger()
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElseGet(List::of);
    }

    public Optional<Boolean> skalHindreTilbaketrekk() {
        return Optional.ofNullable(skalHindreTilbaketrekk);
    }

    public boolean erAktivt() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setBgBeregningsresultatFP(BeregningsresultatEntitet bgBeregningsresultatFP) {
        this.bgBeregningsresultatFP = bgBeregningsresultatFP;
    }

    void setUtbetBeregningsresultatFP(BeregningsresultatEntitet utbetBeregningsresultatFP) {
        this.utbetBeregningsresultatFP = utbetBeregningsresultatFP;
    }

    public void setBeregningsresultatFeriepenger(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        this.beregningsresultatFeriepenger = beregningsresultatFeriepenger;
    }

    void setSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
        this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
    }
}
