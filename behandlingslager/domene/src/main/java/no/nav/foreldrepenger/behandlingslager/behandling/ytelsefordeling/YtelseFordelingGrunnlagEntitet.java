package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

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
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "YtelseFordelingGrunnlag")
@Table(name = "GR_YTELSES_FORDELING")
public class YtelseFordelingGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_YTELSES_FORDELING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "so_fordeling_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittFordelingEntitet oppgittFordeling;

    @ManyToOne
    @JoinColumn(name = "overstyrt_fordeling_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittFordelingEntitet overstyrtFordeling;

    @ManyToOne
    @JoinColumn(name = "justert_fordeling_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittFordelingEntitet justertFordeling;

    @ManyToOne
    @JoinColumn(name = "so_rettighet_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittRettighetEntitet oppgittRettighet;

    @ManyToOne
    @JoinColumn(name = "so_dekningsgrad_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittDekningsgradEntitet oppgittDekningsgrad;

    @ManyToOne
    @JoinColumn(name = "utenomsorg_id", updatable = false, unique = true)
    @ChangeTracked
    private PerioderUtenOmsorgEntitet perioderUtenOmsorgEntitet;

    @ManyToOne
    @JoinColumn(name = "aleneomsorg_id", updatable = false, unique = true)
    @ChangeTracked
    private PerioderAleneOmsorgEntitet perioderAleneOmsorgEntitet;

    @ManyToOne
    @JoinColumn(name = "uttak_dokumentasjon_id", updatable = false, unique = true)
    @ChangeTracked
    private PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon;

    @ManyToOne
    @JoinColumn(name = "annen_forelder_har_rett_id", updatable = false, unique = true)
    @ChangeTracked
    private PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRettEntitet;

    @ManyToOne
    @JoinColumn(name = "mor_stonad_eos_id", updatable = false, unique = true)
    @ChangeTracked
    private PerioderMorStønadEØSEntitet perioderMorStønadEØSEntitet;

    @ManyToOne
    @JoinColumn(name = "opprinnelige_aktkrav_per_id", updatable = false, unique = true)
    @ChangeTracked
    private AktivitetskravPerioderEntitet opprinneligeAktivitetskravPerioder;

    @ManyToOne
    @JoinColumn(name = "saksbehandlede_aktkrav_per_id", updatable = false, unique = true)
    @ChangeTracked
    private AktivitetskravPerioderEntitet saksbehandledeAktivitetskravPerioder;

    @ManyToOne
    @JoinColumn(name = "yf_AVKLART_DATO_id", updatable = false, unique = true)
    @ChangeTracked
    private AvklarteUttakDatoerEntitet avklarteUttakDatoerEntitet;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    YtelseFordelingGrunnlagEntitet() {
    }

    OppgittFordelingEntitet getOppgittFordeling() {
        return oppgittFordeling;
    }

    void setOppgittFordeling(OppgittFordelingEntitet oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
    }

    OppgittFordelingEntitet getOverstyrtFordeling() {
        return overstyrtFordeling;
    }

    void setOverstyrtFordeling(OppgittFordelingEntitet overstyrtFordeling) {
        this.overstyrtFordeling = overstyrtFordeling;
    }

    public OppgittFordelingEntitet getJustertFordeling() {
        return justertFordeling;
    }

    void setJustertFordeling(OppgittFordelingEntitet justertFordeling) {
        this.justertFordeling = justertFordeling;
    }

    OppgittDekningsgradEntitet getOppgittDekningsgrad() {
        return oppgittDekningsgrad;
    }

    void setOppgittDekningsgrad(OppgittDekningsgradEntitet dekningsgrad) {
        this.oppgittDekningsgrad = dekningsgrad;
    }

    OppgittRettighetEntitet getOppgittRettighet() {
        return oppgittRettighet;
    }

    void setOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }


    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    PerioderUtenOmsorgEntitet getPerioderUtenOmsorg() {
        return perioderUtenOmsorgEntitet;
    }

    void setPerioderUtenOmsorg(PerioderUtenOmsorgEntitet perioder) {
        this.perioderUtenOmsorgEntitet = perioder;
    }

    public AktivitetskravPerioderEntitet getOpprinneligeAktivitetskravPerioder() {
        return opprinneligeAktivitetskravPerioder;
    }

    public void setOpprinneligeAktivitetskravPerioder(AktivitetskravPerioderEntitet opprinneligAktivitetskravPerioder) {
        this.opprinneligeAktivitetskravPerioder = opprinneligAktivitetskravPerioder;
    }

    public AktivitetskravPerioderEntitet getSaksbehandledeAktivitetskravPerioder() {
        return saksbehandledeAktivitetskravPerioder;
    }

    public void setSaksbehandledeAktivitetskravPerioder(AktivitetskravPerioderEntitet saksbehandletAktivitetskravPerioder) {
        this.saksbehandledeAktivitetskravPerioder = saksbehandletAktivitetskravPerioder;
    }

    PerioderAleneOmsorgEntitet getPerioderAleneOmsorgEntitet() {
        return perioderAleneOmsorgEntitet;
    }

    void setPerioderAleneOmsorg(PerioderAleneOmsorgEntitet perioder) {
        this.perioderAleneOmsorgEntitet = perioder;
    }

    PerioderUttakDokumentasjonEntitet getPerioderUttakDokumentasjon() {
        return perioderUttakDokumentasjon;
    }

    void setPerioderUttakDokumentasjon(PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon) {
        this.perioderUttakDokumentasjon = perioderUttakDokumentasjon;
    }

    PerioderAnnenforelderHarRettEntitet getPerioderAnnenforelderHarRettEntitet() {
        return perioderAnnenforelderHarRettEntitet;
    }

    void setPerioderAnnenforelderHarRettEntitet(PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRettEntitet) {
        this.perioderAnnenforelderHarRettEntitet = perioderAnnenforelderHarRettEntitet;
    }

    PerioderMorStønadEØSEntitet getPerioderMorStønadEØSEntitet() {
        return perioderMorStønadEØSEntitet;
    }

    void setPerioderMorStønadEØSEntitet(PerioderMorStønadEØSEntitet perioderMorStønadEØSEntitet) {
        this.perioderMorStønadEØSEntitet = perioderMorStønadEØSEntitet;
    }

    AvklarteUttakDatoerEntitet getAvklarteUttakDatoer() {
        return avklarteUttakDatoerEntitet;
    }

    void setAvklarteUttakDatoerEntitet(AvklarteUttakDatoerEntitet avklarteUttakDatoerEntitet) {
        this.avklarteUttakDatoerEntitet = avklarteUttakDatoerEntitet;
    }

    /* eksponeres ikke public for andre. */
    Long getId() {
        return id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (YtelseFordelingGrunnlagEntitet) o;
        return aktiv == that.aktiv &&
            Objects.equals(oppgittFordeling, that.oppgittFordeling) &&
            Objects.equals(oppgittRettighet, that.oppgittRettighet) &&
            Objects.equals(oppgittDekningsgrad, that.oppgittDekningsgrad) &&
            Objects.equals(perioderUtenOmsorgEntitet, that.perioderUtenOmsorgEntitet) &&
            Objects.equals(perioderAleneOmsorgEntitet, that.perioderAleneOmsorgEntitet) &&
            Objects.equals(perioderAnnenforelderHarRettEntitet, that.perioderAnnenforelderHarRettEntitet) &&
            Objects.equals(perioderMorStønadEØSEntitet, that.perioderMorStønadEØSEntitet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFordeling, oppgittRettighet, oppgittDekningsgrad, perioderUtenOmsorgEntitet,
            perioderAleneOmsorgEntitet, perioderAnnenforelderHarRettEntitet, perioderMorStønadEØSEntitet, aktiv);
    }
}
