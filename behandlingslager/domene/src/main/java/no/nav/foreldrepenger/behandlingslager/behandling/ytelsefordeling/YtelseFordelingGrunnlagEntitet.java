package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
    @JoinColumn(name = "overstyrt_rettighet_id", updatable = false, unique = true)
    @ChangeTracked
    private OppgittRettighetEntitet overstyrtRettighet;

    @Column(name = "overstyrt_rettighetstype")
    @ChangeTracked
    @Enumerated(EnumType.STRING)
    private Rettighetstype overstyrtRettighetstype;

    @Column(name = "oppgitt_dekningsgrad")
    @Max(value = 100)
    @Min(value = 0)
    @ChangeTracked
    private Integer oppgittDekningsgrad;

    @Column(name = "sakskompleks_dekningsgrad")
    @Max(value = 100)
    @Min(value = 0)
    private Integer sakskompleksDekningsgrad;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt_omsorg")
    @ChangeTracked
    private Boolean overstyrtOmsorg;

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

    void setOppgittDekningsgrad(Integer dekningsgrad) {
        this.oppgittDekningsgrad = dekningsgrad;
    }

    public Integer getOppgittDekningsgrad() {
        return oppgittDekningsgrad;
    }

    public Integer getSakskompleksDekningsgrad() {
        return sakskompleksDekningsgrad;
    }

    public void setSakskompleksDekningsgrad(Integer sakskompleksDekningsgrad) {
        this.sakskompleksDekningsgrad = sakskompleksDekningsgrad;
    }

    OppgittRettighetEntitet getOppgittRettighet() {
        return oppgittRettighet;
    }

    void setOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
    }

    public OppgittRettighetEntitet getOverstyrtRettighet() {
        return overstyrtRettighet;
    }

    void setOverstyrtRettighet(OppgittRettighetEntitet overstyrtRettighet) {
        this.overstyrtRettighet = overstyrtRettighet;
    }

    public Rettighetstype getOverstyrtRettighetstype() {
        return overstyrtRettighetstype;
    }

    public void setOverstyrtRettighetstype(Rettighetstype overstyrtRettighetstype) {
        this.overstyrtRettighetstype = overstyrtRettighetstype;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }


    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    void setOverstyrtOmsorg(Boolean overstyrtOmsorg) {
        this.overstyrtOmsorg = overstyrtOmsorg;
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
            Objects.equals(oppgittDekningsgrad, that.oppgittDekningsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFordeling, oppgittRettighet, oppgittDekningsgrad, aktiv);
    }
}
