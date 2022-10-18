package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "SvpTilretteleggingEntitet")
@Table(name = "SVP_TILRETTELEGGING")
public class SvpTilretteleggingEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_TILRETTELEGGING")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SVP_TILRETTELEGGINGER_ID", nullable = false, updatable = false, unique = true)
    private SvpTilretteleggingerEntitet tilrettelegginger;

    @Column(name = "BEHOV_FOM", nullable = false)
    private LocalDate behovForTilretteleggingFom;

    @OneToMany(cascade = CascadeType.ALL)
    @BatchSize(size = 25)
    @JoinColumn(name = "SVP_TILRETTELEGGING_ID")
    private List<TilretteleggingFOM> tilretteleggingFOMListe = new ArrayList<>();

    @Convert(converter = ArbeidType.KodeverdiConverter.class)
    @Column(name="arbeid_type", nullable = false)
    @ChangeTracked
    private ArbeidType arbeidType = ArbeidType.UDEFINERT;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Column(name = "OPPLYSNINGER_RISIKO")
    private String opplysningerOmRisikofaktorer;

    @Column(name = "OPPLYSNINGER_TILTAK")
    private String opplysningerOmTilretteleggingstiltak;

    @Column(name = "BEGRUNNELSE")
    private String begrunnelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "KOPIERT_FRA_TDLG_BEH", nullable = false)
    private Boolean kopiertFraTidligereBehandling;

    @Column(name = "MOTTATT_TIDSPUNKT", nullable = false)
    private LocalDateTime mottattTidspunkt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "SKAL_BRUKES", nullable = false)
    private boolean skalBrukes = true;

    @Embedded
    private InternArbeidsforholdRef internArbeidsforholdRef;

    public SvpTilretteleggingEntitet() {
        //jaja
    }

    public SvpTilretteleggingEntitet(SvpTilretteleggingEntitet svpTilrettelegging, SvpTilretteleggingerEntitet opprinneligeTilrettelegginger) {
        this.behovForTilretteleggingFom = svpTilrettelegging.getBehovForTilretteleggingFom();
        this.tilrettelegginger = opprinneligeTilrettelegginger;
        svpTilrettelegging.getTilretteleggingFOMListe().stream()
            .map(fom -> new TilretteleggingFOM.Builder(fom).build())
            .forEach(fom -> this.tilretteleggingFOMListe.add(fom));
        this.arbeidType = svpTilrettelegging.getArbeidType();
        this.arbeidsgiver = svpTilrettelegging.getArbeidsgiver().orElse(null);
        this.opplysningerOmRisikofaktorer = svpTilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null);
        this.opplysningerOmTilretteleggingstiltak = svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null);
        this.begrunnelse = svpTilrettelegging.getBegrunnelse().orElse(null);
        this.kopiertFraTidligereBehandling = svpTilrettelegging.getKopiertFraTidligereBehandling();
        this.mottattTidspunkt = svpTilrettelegging.getMottattTidspunkt();
        this.internArbeidsforholdRef = svpTilrettelegging.getInternArbeidsforholdRef().orElse(null);
        this.skalBrukes = svpTilrettelegging.getSkalBrukes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (SvpTilretteleggingEntitet) o;
        return Objects.equals(behovForTilretteleggingFom, that.behovForTilretteleggingFom) &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(internArbeidsforholdRef, that.internArbeidsforholdRef) &&
            Objects.equals(tilretteleggingFOMListe, that.tilretteleggingFOMListe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behovForTilretteleggingFom, tilretteleggingFOMListe, arbeidsgiver);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getBehovForTilretteleggingFom() {
        return behovForTilretteleggingFom;
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public Optional<String> getOpplysningerOmRisikofaktorer() {
        return Optional.ofNullable(opplysningerOmRisikofaktorer);
    }

    public Optional<String> getBegrunnelse() {
        return Optional.ofNullable(begrunnelse);
    }

    public Optional<String> getOpplysningerOmTilretteleggingstiltak() {
        return Optional.ofNullable(opplysningerOmTilretteleggingstiltak);
    }

    public Boolean getKopiertFraTidligereBehandling() {
        return kopiertFraTidligereBehandling;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public Optional<InternArbeidsforholdRef> getInternArbeidsforholdRef() {
        return Optional.ofNullable(internArbeidsforholdRef);
    }

    public boolean getSkalBrukes() {
        return skalBrukes;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(id);
    }

    public List<TilretteleggingFOM> getTilretteleggingFOMListe() {
        return tilretteleggingFOMListe;
    }

    public static class Builder {

        private SvpTilretteleggingEntitet mal;

        public Builder() {
            this(new SvpTilretteleggingEntitet());
        }

        public static Builder fraEksisterende(SvpTilretteleggingEntitet tilrettelegging) {
            return new Builder()
                .medArbeidsgiver(tilrettelegging.getArbeidsgiver().orElse(null))
                .medArbeidType(tilrettelegging.getArbeidType())
                .medBehovForTilretteleggingFom(tilrettelegging.behovForTilretteleggingFom)
                .medBegrunnelse(tilrettelegging.getBegrunnelse().orElse(null))
                .medArbeidType(tilrettelegging.getArbeidType())
                .medArbeidsgiver(tilrettelegging.getArbeidsgiver().orElse(null))
                .medInternArbeidsforholdRef(tilrettelegging.getInternArbeidsforholdRef().orElse(null))
                .medKopiertFraTidligereBehandling(true)
                .medOpplysningerOmRisikofaktorer(tilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null))
                .medOpplysningerOmTilretteleggingstiltak(tilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null))
                .medSkalBrukes(tilrettelegging.getSkalBrukes())
                .medMottattTidspunkt(tilrettelegging.getMottattTidspunkt())
                .medTilretteleggingFraDatoer(tilrettelegging.getTilretteleggingFOMListe());
        }
        public Builder(SvpTilretteleggingEntitet tilretteleggingEntitet) {
            mal = new SvpTilretteleggingEntitet(tilretteleggingEntitet, null);
            if (mal.tilretteleggingFOMListe == null) {
                mal.tilretteleggingFOMListe = new ArrayList<>();
            }
        }

        public Builder medBehovForTilretteleggingFom(LocalDate behovForTilretteleggingFom) {
            this.mal.behovForTilretteleggingFom = behovForTilretteleggingFom;
            return this;
        }

        public Builder medHelTilrettelegging(LocalDate helTilretteleggingFom, LocalDate tidligstMottatt) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.HEL_TILRETTELEGGING)
                .medFomDato(helTilretteleggingFom)
                .medTidligstMottattDato(tidligstMottatt)
                .build();
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medDelvisTilrettelegging(LocalDate delvisTilretteleggingFom, BigDecimal stillingsprosent, LocalDate tidligstMottatt) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(delvisTilretteleggingFom)
                .medStillingsprosent(stillingsprosent)
                .medTidligstMottattDato(tidligstMottatt)
                .build();
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medIngenTilrettelegging(LocalDate slutteArbeidFom, LocalDate tidligstMottatt) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .medFomDato(slutteArbeidFom)
                .medTidligstMottattDato(tidligstMottatt)
                .build();
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medTilretteleggingFraDatoer(List<TilretteleggingFOM> tilretteleggingFraDatoer) {
            mal.tilretteleggingFOMListe = tilretteleggingFraDatoer;
            return this;
        }

        public Builder medInternArbeidsforholdRef(InternArbeidsforholdRef internArbeidsforholdRef) {
            this.mal.internArbeidsforholdRef = internArbeidsforholdRef;
            return this;
        }

        public Builder medTilretteleggingFom(TilretteleggingFOM tilretteleggingFOM) {
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medArbeidType(ArbeidType arbeidType) {
            this.mal.arbeidType = arbeidType;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            this.mal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medOpplysningerOmRisikofaktorer(String opplysningerOmRisikofaktorer) {
            this.mal.opplysningerOmRisikofaktorer = opplysningerOmRisikofaktorer;
            return this;
        }

        public Builder medOpplysningerOmTilretteleggingstiltak(String opplysningerOmTilretteleggingstiltak) {
            this.mal.opplysningerOmTilretteleggingstiltak = opplysningerOmTilretteleggingstiltak;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            this.mal.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medKopiertFraTidligereBehandling(boolean kopiertFraTidligereBehandling) {
            this.mal.kopiertFraTidligereBehandling = kopiertFraTidligereBehandling;
            return this;
        }

        public Builder medMottattTidspunkt(LocalDateTime mottattTidspunkt) {
            this.mal.mottattTidspunkt = mottattTidspunkt;
            return this;
        }

        public Builder medSkalBrukes(boolean skalBrukes) {
            this.mal.skalBrukes = skalBrukes;
            return this;
        }

        public SvpTilretteleggingEntitet build() {
            return this.mal;
        }
    }
}
