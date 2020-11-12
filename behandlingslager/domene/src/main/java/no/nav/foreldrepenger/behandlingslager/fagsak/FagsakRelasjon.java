package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "FagsakRelasjon")
@Table(name = "FAGSAK_RELASJON")
public class FagsakRelasjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_RELASJON")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fagsak_en_id", nullable = false)
    private Fagsak fagsakNrEn;

    @ManyToOne
    @JoinColumn(name = "fagsak_to_id")
    private Fagsak fagsakNrTo;

    @ManyToOne
    @JoinColumn(name = "konto_beregning_id")
    private Stønadskontoberegning stønadskontoberegning;

    @ManyToOne
    @JoinColumn(name = "overstyrt_konto_beregning_id")
    private Stønadskontoberegning overstyrtStønadskontoberegning;

    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "dekningsgrad", nullable = false)))
    @Embedded
    private Dekningsgrad dekningsgrad;

    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "overstyrt_dekningsgrad")))
    @Embedded
    private Dekningsgrad overstyrtDekningsgrad;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "avsluttningsdato")
    private LocalDate avsluttningsdato;

    FagsakRelasjon() {
        // For Hibernate
    }

    public FagsakRelasjon(Fagsak fagsakNrEn,
                   Fagsak fagsakNrTo,
                   Stønadskontoberegning stønadskontoberegning,
                   Stønadskontoberegning overstyrtStønadskontoberegning,
                   Dekningsgrad dekningsgrad,
                   Dekningsgrad overstyrtDekningsgrad,
                   LocalDate avsluttningsdato) {
        this.stønadskontoberegning = stønadskontoberegning;
        this.fagsakNrEn = fagsakNrEn;
        this.fagsakNrTo = fagsakNrTo;
        this.overstyrtStønadskontoberegning = overstyrtStønadskontoberegning;
        this.dekningsgrad = dekningsgrad;
        this.overstyrtDekningsgrad = overstyrtDekningsgrad;
        this.avsluttningsdato = avsluttningsdato;
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

    public Optional<Stønadskontoberegning> getStønadskontoberegning() {
        return Optional.ofNullable(stønadskontoberegning);
    }

    public Fagsak getFagsakNrEn() {
        return fagsakNrEn;
    }

    public Optional<Fagsak> getFagsakNrTo() {
        return Optional.ofNullable(fagsakNrTo);
    }

    public Optional<Fagsak> getRelatertFagsakFraId(Long fagsakId) {
        if (fagsakNrEn.getId().equals(fagsakId)) {
            return getFagsakNrTo();
        } else if (fagsakNrTo != null && fagsakNrTo.getId().equals(fagsakId)) {
            return Optional.of(fagsakNrEn);
        }
        return Optional.empty();
    }

    public Optional<Fagsak> getRelatertFagsak(Fagsak fagsak) {
        return getRelatertFagsakFraId(fagsak.getId());
    }


    public Dekningsgrad getDekningsgrad() {
        return dekningsgrad;
    }

    public Optional<Dekningsgrad> getOverstyrtDekningsgrad() {
        return Optional.ofNullable(overstyrtDekningsgrad);
    }

    public Optional<Stønadskontoberegning> getGjeldendeStønadskontoberegning() {
        return getOverstyrtStønadskontoberegning().isPresent() ? getOverstyrtStønadskontoberegning() : getStønadskontoberegning();
    }

    public Dekningsgrad getGjeldendeDekningsgrad() {
        return getOverstyrtDekningsgrad().orElse(getDekningsgrad());
    }

    public Optional<Stønadskontoberegning> getOverstyrtStønadskontoberegning() {
        return Optional.ofNullable(overstyrtStønadskontoberegning);
    }

    public LocalDate getAvsluttningsdato() {
        return avsluttningsdato;
    }
}
