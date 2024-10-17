package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "SvpTilretteleggingEntitet")
@Table(name = "SVP_TILRETTELEGGING")
public class SvpTilretteleggingEntitet extends BaseEntitet {

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

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "SVP_TILRETTELEGGING_ID")
    private List<SvpAvklartOpphold> avklarteOpphold = new ArrayList<>();

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
        svpTilrettelegging.getAvklarteOpphold().stream()
            .map(avklartOpphold -> SvpAvklartOpphold.Builder.fraEksisterende(avklartOpphold).build())
            .forEach(avklartOpphold -> this.avklarteOpphold.add(avklartOpphold));
    }

    @Override
    public boolean equals(Object o) {
        return erLikUtenomTilrettelegginger(o) && o instanceof SvpTilretteleggingEntitet that &&
            Objects.equals(tilretteleggingFOMListe, that.tilretteleggingFOMListe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behovForTilretteleggingFom, tilretteleggingFOMListe, arbeidType, arbeidsgiver, internArbeidsforholdRef);
    }

    public boolean erLik(Object o) {
        return erLikUtenomTilrettelegginger(o) && o instanceof SvpTilretteleggingEntitet that &&
            tilretteleggingFOMListe.size() == that.tilretteleggingFOMListe.size() &&
            new HashSet<>(tilretteleggingFOMListe).containsAll(that.tilretteleggingFOMListe);
    }


    private boolean erLikUtenomTilrettelegginger(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (SvpTilretteleggingEntitet) o;
        return Objects.equals(behovForTilretteleggingFom, that.behovForTilretteleggingFom) &&
            Objects.equals(arbeidType, that.arbeidType) &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(internArbeidsforholdRef, that.internArbeidsforholdRef);
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

    public List<TilretteleggingFOM> getTilretteleggingFOMListe() {
        return tilretteleggingFOMListe;
    }

    public List<SvpAvklartOpphold> getAvklarteOpphold() {
        return avklarteOpphold;
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
            if (mal.avklarteOpphold == null) {
                mal.avklarteOpphold = new ArrayList<>();
            }
        }

        public Builder medBehovForTilretteleggingFom(LocalDate behovForTilretteleggingFom) {
            this.mal.behovForTilretteleggingFom = behovForTilretteleggingFom;
            return this;
        }

        public Builder medHelTilrettelegging(LocalDate helTilretteleggingFom, LocalDate tidligstMottatt, SvpTilretteleggingFomKilde kilde) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.HEL_TILRETTELEGGING)
                .medFomDato(helTilretteleggingFom)
                .medTidligstMottattDato(tidligstMottatt)
                .medKilde(kilde)
                .build();
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medDelvisTilrettelegging(LocalDate delvisTilretteleggingFom, BigDecimal stillingsprosent, LocalDate tidligstMottatt,
                                                SvpTilretteleggingFomKilde kilde) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(delvisTilretteleggingFom)
                .medStillingsprosent(stillingsprosent)
                .medTidligstMottattDato(tidligstMottatt)
                .medKilde(kilde)
                .build();
            mal.tilretteleggingFOMListe.add(tilretteleggingFOM);
            return this;
        }

        public Builder medIngenTilrettelegging(LocalDate slutteArbeidFom, LocalDate tidligstMottatt, SvpTilretteleggingFomKilde kilde) {
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .medFomDato(slutteArbeidFom)
                .medTidligstMottattDato(tidligstMottatt)
                .medKilde(kilde)
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
        public Builder medAvklartOpphold(SvpAvklartOpphold avklarteOpphold) {
            this.mal.avklarteOpphold.add(avklarteOpphold);
            return this;
        }

        public Builder medAvklarteOpphold(List<SvpAvklartOpphold> avklarteOpphold) {
            this.mal.avklarteOpphold = avklarteOpphold;
            return this;
        }

        public SvpTilretteleggingEntitet build() {
            return this.mal;
        }
    }
}
