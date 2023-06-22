package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Samle betegnelse for Fødsel, Adopsjon, Omsorgsovertakelse og Terminbekreftelse.
 *
 * Fødsler ligger i listen med UidentifisertBarn
 * Barn som skal adopteres / overdra omsorgen for ligger i listen med UidentifisertBarn
 */
@Table(name = "FH_FAMILIE_HENDELSE")
@Entity(name = "FamilieHendelse")
public class FamilieHendelseEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAMILIE_HENDELSE")
    private Long id;

    @ChangeTracked
    @OneToOne(mappedBy = "familieHendelse")
    private AdopsjonEntitet adopsjon;

    @ChangeTracked
    @OneToOne(mappedBy = "familieHendelse")
    private TerminbekreftelseEntitet terminbekreftelse;

    @ChangeTracked
    @OneToMany(mappedBy = "familieHendelse")
    private List<UidentifisertBarnEntitet> barna = new ArrayList<>();

    @ChangeTracked
    @Column(name = "antall_barn")
    private Integer antallBarn;

    @ChangeTracked
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "mor_for_syk_ved_fodsel")
    private Boolean morForSykVedFødsel;

    @ChangeTracked
    @Convert(converter = FamilieHendelseType.KodeverdiConverter.class)
    @Column(name="familie_hendelse_type", nullable = false)
    private FamilieHendelseType type = FamilieHendelseType.UDEFINERT;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FamilieHendelseEntitet() {
    }

    FamilieHendelseEntitet(FamilieHendelseType type) {
        this.type = type;
    }

    FamilieHendelseEntitet(FamilieHendelseEntitet hendelse) {
        this.type = hendelse.getType();
        this.antallBarn = hendelse.getAntallBarn();
        this.morForSykVedFødsel = hendelse.erMorForSykVedFødsel();

        hendelse.getAdopsjon().ifPresent(it -> {
            final var nyAdopsjon = new AdopsjonEntitet(it);
            nyAdopsjon.setFamilieHendelse(this);
            this.setAdopsjon(nyAdopsjon);
        });
        hendelse.getTerminbekreftelse().ifPresent(it -> {
            final var nyTerminbekreftelse = new TerminbekreftelseEntitet(it);
            nyTerminbekreftelse.setFamilieHendelse(this);
            this.setTerminbekreftelse(nyTerminbekreftelse);
        });


        var barnRelatertTilHendelse = this.barna.stream().collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, Function.identity()));
        for (var barn : hendelse.getBarna()) {
            var lagretBarn = barnRelatertTilHendelse.get(barn.getBarnNummer());
            if (lagretBarn == null) {
                var kopi = new UidentifisertBarnEntitet(barn);
                kopi.setFamilieHendelse(this);
                this.barna.add(kopi);
            } else {
                lagretBarn.deepCopy(barn);
            }
        }
    }

    public Long getId() {
        return id;
    }

    /**
     * Liste over Uidentifiserte barn, dvs barn uten fnr. Dette betyr ikke at de ikke har fnr men at de ikke er identifisert med det i Behandlingen
     * @return Liste over barn
     */
    public Integer getAntallBarn() {
        return antallBarn;
    }

    void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }


    public List<UidentifisertBarn> getBarna() {
        return Collections.unmodifiableList(barna);
    }

    void leggTilBarn(UidentifisertBarn barn) {
        final var barnEntitet = (UidentifisertBarnEntitet) barn;
        this.barna.add(barnEntitet);
        barnEntitet.setFamilieHendelse(this);
    }

    void clearBarn() {
        this.barna.clear();
    }

    /**
     * Data vedrørende terminbekreftelsen som er relevant for behandlingen
     * @return terminbekreftelsen
     */
    public Optional<TerminbekreftelseEntitet> getTerminbekreftelse() {
        return Optional.ofNullable(terminbekreftelse);
    }

    void setTerminbekreftelse(TerminbekreftelseEntitet terminbekreftelse) {
        this.terminbekreftelse = terminbekreftelse;
        if (terminbekreftelse != null) {
            this.terminbekreftelse.setFamilieHendelse(this);
        }
    }

    /**
     * Data vedrørende adopsjonsbekreftelsen / omsorgsovertakelse som er relevant for behandlingen.
     * @return adopsjon
     */
    public Optional<AdopsjonEntitet> getAdopsjon() {
        return Optional.ofNullable(adopsjon);
    }

    void setAdopsjon(AdopsjonEntitet adopsjon) {
        this.adopsjon = adopsjon;
        if (adopsjon != null) {
            this.adopsjon.setFamilieHendelse(this);
        }
    }

    /**
     * Internt kodeverk som identifiserer hva søknaden er basert på. F.eks basert på føsel.
     * @return FamilieHendelseTypen
     */
    public FamilieHendelseType getType() {
        return type;
    }

    void setType(FamilieHendelseType type) {
        this.type = type;
    }

    /**
     * Henter ut fødselsdatoen fra listen over UidentifiserteBarn hvis typen er Føsel
     * @return Fødselsdatoen
     */
    public Optional<LocalDate> getFødselsdato() {
        if (type.equals(FamilieHendelseType.FØDSEL)) {
            return barna.stream().map(UidentifisertBarnEntitet::getFødselsdato).findFirst();
        }
        return Optional.empty();
    }

    /**
     * Henter ut termindato fra eventuell terminbekreftelse
     * @return termindatoen
     */
    public Optional<LocalDate> getTermindato() {
        if (FamilieHendelseType.gjelderFødsel(type)) {
            return getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        }
        return Optional.empty();
    }

    /**
     * Sjekker om det er født et barn med dødsdato på samme dag som det er født.
     * @return boolean
     */
    public boolean getInnholderDøfødtBarn() {
        if (type.equals(FamilieHendelseType.FØDSEL)) {
            return barna.stream().anyMatch(barn -> barn.getDødsdato().isPresent()
                && barn.getDødsdato().get().equals(barn.getFødselsdato()));
        }
        return false;
    }

    /**
     * Sjekker om hendelsen omhandler et dødt barn.
     * @return boolean
     */
    public boolean getInnholderDødtBarn() {
        if (type.equals(FamilieHendelseType.FØDSEL)) {
            return barna.stream().anyMatch(barn -> barn.getDødsdato().isPresent());
        }
        return false;
    }

    /**
     * Henter ut oppgitt / vurdert status om mor er for syk til å ta seg av barnet
     * @return boolean
     */
    public Boolean erMorForSykVedFødsel() {
        return morForSykVedFødsel;
    }

    /**
     * Beregnet skjæringstidspunkt for hendelsen.
     * - Termindato
     * - Fødselsdato
     * - Omsorgsovertakelse dato
     * - Foreldreansvars dato
     * - Dato for stebarnsadopsjon
     *
     * NB: Tar ikke hensyn til perioder med permisjon.
     *
     * @return Skjæringstidspunktet for hendelse
     */
    public LocalDate getSkjæringstidspunkt() {
        if (FamilieHendelseType.TERMIN.equals(type)) {
            return terminbekreftelse.getTermindato();
        }
        if (FamilieHendelseType.FØDSEL.equals(type)) {
            // Dersom antall barn satt til 0 så brukes eventuell termindato
            return getFødselsdato().or(this::getTermindato).orElse(null);
        }
        if (FamilieHendelseType.ADOPSJON.equals(type) || FamilieHendelseType.OMSORG.equals(type)) {
            return adopsjon.getOmsorgsovertakelseDato();
        }
        throw new IllegalStateException("Utvikler feil: ukjent hendelsestype: " + type);
    }

    /**
     * Vurderer om hendelsen er av typen fødsel
     * @return true/false
     */
    public boolean getGjelderFødsel() {
        return FamilieHendelseType.gjelderFødsel(type);
    }

    /**
     * Vurderer om hendelsen er av typen adopsjon/omsorgovertakelse
     * @return true/false
     */
    public boolean getGjelderAdopsjon() {
        return FamilieHendelseType.gjelderAdopsjon(type);
    }

    void setMorForSykVedFødsel(Boolean erMorForSykVedFødsel) {
        this.morForSykVedFødsel = erMorForSykVedFødsel;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (FamilieHendelseEntitet) o;
        return Objects.equals(antallBarn, that.antallBarn) &&
                Objects.equals(adopsjon, that.adopsjon) &&
                Objects.equals(terminbekreftelse, that.terminbekreftelse) &&
                Objects.equals(barna, that.barna) &&
                Objects.equals(type, that.type) &&
                Objects.equals(morForSykVedFødsel, that.morForSykVedFødsel);
    }


    @Override
    public int hashCode() {
        return Objects.hash(adopsjon, terminbekreftelse, barna, antallBarn, type, morForSykVedFødsel);
    }


    @Override
    public String toString() {
        return "FamilieHendelseEntitet{" +
                "id=" + id +
                ", adopsjon=" + adopsjon +
                ", terminbekreftelse=" + terminbekreftelse +
                ", barna=" + barna +
                ", antallBarn=" + antallBarn +
                ", type=" + type +
                ", morForSykVedFødsel=" + morForSykVedFødsel +
                '}';
    }
}
