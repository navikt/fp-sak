package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @AttributeOverride(name = "verdi", column = @Column(name = "dekningsgrad"))
    @Embedded
    private Dekningsgrad dekningsgrad;

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
                          Dekningsgrad dekningsgrad,
                          LocalDate avsluttningsdato) {
        this.stønadskontoberegning = stønadskontoberegning;
        this.fagsakNrEn = fagsakNrEn;
        this.fagsakNrTo = fagsakNrTo;
        this.dekningsgrad = dekningsgrad;
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
        }
        if (fagsakNrTo != null && fagsakNrTo.getId().equals(fagsakId)) {
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

    public LocalDate getAvsluttningsdato() {
        return avsluttningsdato;
    }
}
